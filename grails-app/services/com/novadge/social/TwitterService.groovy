package com.novadge.social

import grails.converters.JSON
import grails.transaction.Transactional
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import sun.misc.BASE64Encoder
import twitter4j.*
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import twitter4j.conf.ConfigurationBuilder

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.util.TimeZone;

@Transactional
class TwitterService {


    public final static String urlBase = "https://api.twitter.com/";
    public final static String apiVersion = "1.1/";
    public final static String statusUrl = urlBase + apiVersion + "statuses/";
    public final static String favUrl = urlBase + apiVersion + "favorites/";
    public final static String bearerUrl = urlBase + "oauth2/token";
    private static final String WWW_DETAILS = "See http://twitter4j.org/en/configuration.html for details";


    final String CONST_ENV_NAME = "env-beta"
    // All Account Activity API endpoints
    String URL_CREATE_WEBHOOK_FMT = "account_activity/all/$CONST_ENV_NAME/webhooks.json" //webhook callback url
    String URL_DELETE_WEBHOOK_FMT = "account_activity/all/$CONST_ENV_NAME/webhooks/%s.json" // [webhook_id]
    String URL_UPDATE_WEBHOOK_FMT = "account_activity/all/$CONST_ENV_NAME/webhooks/%s.json" // [webhook_id]
    String URL_GET_WEBHOOK_FMT = "account_activity/all/$CONST_ENV_NAME/webhooks.json"
    String URL_GET_WEBHOOK_ID_FMT = "account_activity/all/webhooks.json"
    String URL_UNSUBSCRIBE_FROM_WEBHOOK_FMT = "account_activity/all/$CONST_ENV_NAME/subscriptions.json"
    String URL_SUBSCRIBE_TO_WEBHOOK_FMT = "account_activity/all/$CONST_ENV_NAME/subscriptions.json" //callback url
    String URL_GET_WEBHOOK_SUBSCRIPTION_FMT = "account_activity/all/$CONST_ENV_NAME/subscriptions/list.json"


    static String getStatusUrl() {
        return statusUrl;
    }

    static String getOAuthUrl() { return bearerUrl; }

    static String getFavUrl() { return favUrl; }

    /**
     * Used to get a twitter4j Twitter object
     * @param oAuthConsumerKey : Application twitter consumer key
     * @param oAuthConsumerSecret : Application consumer secret
     * @returns Twitter object
     * */
    Twitter getTwitter(String oAuthConsumerKey, String oAuthConsumerSecret) {
        //create config object
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(oAuthConsumerKey)
                .setOAuthConsumerSecret(oAuthConsumerSecret)
                .setTweetModeExtended(true)

        //use config object to get twitter factory object
        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance();
        return twitter
    }

    /**
     * Used to get a twitter4j Twitter object
     * @param oAuthConsumerKey : Application twitter consumer key
     * @param oAuthConsumerSecret : Application consumer secret
     * @param oAuthAccessToken : User or application twitter access token
     * @param oAuthAccessTokenSecret : User or application access token secret
     * @returns Twitter : twitter object
     * */
    Twitter getTwitter(String oAuthConsumerKey, String oAuthConsumerSecret,
                       String oAuthAccessToken, String oAuthAccessTokenSecret, boolean isApplicationOnlyAuth = false) {
        // create configuration builder and set properties
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(oAuthConsumerKey)
                .setOAuthConsumerSecret(oAuthConsumerSecret)
                .setOAuthAccessToken(oAuthAccessToken)
                .setOAuthAccessTokenSecret(oAuthAccessTokenSecret)
                .setApplicationOnlyAuthEnabled(isApplicationOnlyAuth)
                .setTweetModeExtended(true)

        TwitterFactory tf = new TwitterFactory(cb.build());
        // get twitter instance
        Twitter twitter = tf.getInstance();
        return twitter
    }

    /**
     * @param props.callbackUrl
     * @param twitterMap.consumerKey
     * @param twitterMap.consumerSecret
     * */

    RequestToken getOAuthRequestToken(Map props, Map twitterMap) {
        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret)
        RequestToken requestToken = null
        if (props.callbackUrl) {
            return twitter.getOAuthRequestToken(props.callbackUrl);
        } else {
            return twitter.getOAuthRequestToken()
        }


    }

    String getAuthorizationURL(RequestToken req) {
        return req.getAuthorizationURL()


    }

    /**
     * Get oAuth access token and secret
     * @param props.oAuthVerifier
     * @param props.requestToken
     * @param twitterMap.consumerKey
     * @param twitterMap.consumerSecret
     * */
    Map getOAuthAccessToken(Map props, Map twitterMap) {
        // get twitter object
        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret)

        // get access token
        AccessToken accessToken = twitter.getOAuthAccessToken(props.requestToken, props.oAuthVerifier)
        // return access token properties in a map
        Map result = [:]
        result.put('screenName', accessToken.getScreenName())
        result.put('userId', accessToken.getUserId())
        result.put('accessToken', accessToken.getToken())
        result.put('accessTokenSecret', accessToken.getTokenSecret())
        return result
    }

    /**
     * Get rate limit status
     *
     * */
    Map rateLimitStatus(String endpoint, Map twitterMap) {
        // get twitter object
        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret,
                twitterMap.accessToken, twitterMap.accessTokenSecret)

        Map<String, RateLimitStatus> rateLimitStatus = twitter.getRateLimitStatus();


        return rateLimitStatus
    }

//    
    /**
     * search for tweets from a user timeline 
     * @param props.query : search query eg "source:twitter4j yusukey"

     * @param twitterMap : configured twitter object
     * */
    List<Map> search(Map props, Map twitterMap) {
        // get twitter object
        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)
        long count = props.count != null ? props.count as long : 100;
        //Pages are not the best way to go.
        // If you are trying to get the 1000 most recent tweets in a search,
        // for example, you should use query.setMaxId() to the lowest # - 1
        // from your previous batch. If you are trying to get new(er)
        // tweets since your last query, you should use query.setSinceId()
        // to start from the value returned in result.getMaxId() from the previous batch
        long maxId = 8//= props.maxId != null? props.maxId as long: 0;

        Query query = new Query(props.query);

        QueryResult result = twitter.search(query);
        List tweets = [];
        List tempTweets = [];
        int retrieved = 0;
        int k = 0;
        while (retrieved < count) {
            tempTweets = formatStatus(result.getTweets());
            tempTweets.sort({ it.id })//sort the result by tweet id
            tweets += tempTweets;
            maxId = result.getMaxId()
            // take the largest tweet id from the list of tweets
            query = new Query(props.query);
            query.setMaxId(maxId);
            retrieved = tweets.size();


        }

        return tweets;

    }

    /**
     * 1. Get all tweets to the creator of the subject tweet, more recent than the subject tweet
     * 2. Filter out (and return_) the tweets which are in reply to the subject tweet
     * @param twitterMap: credentials
     * @param tweetId: The id of the original tweet
     * @param maxId: The inclusive upper bound ID of the latest tweet which should be returned. Set to zero to return most recent results
     * @param screenName: The screenName of the user who created the initial tweet
     * @return
     */
    List<Map> getTweetReplies(Map twitterMap, long tweetId, long maxId, String screenName) {
        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)
        Query query = new Query(query: "to:$screenName", sinceId: tweetId, count: 100);

        if(query.maxId)
            query.maxId(maxId)

        QueryResult result = twitter.search(query);
        List tweets = []
        tweets += formatStatus(result.getTweets())

        while(result.getTweets().size() == 100){
            tweets += getTweetReplies(twitterMap, tweetId, tweets.min { it.id }.id - 1, screenName)
        }

        def results =  tweets.findAll {
            it.inReplyToStatusId == tweetId
        }
        results
    }

    /**
     * get statuses ( tweets ) from a user timeline 
     * @param props.page : page number
     * @param props.count : number of statuses
     * @param props.sinceId : status id to start from
     * @param props.maxId : status id to stop at
     * @param twitterMap : configured twitter object
     * */
    List<Map> getUserTimeline(Map props, Map twitterMap) {


        int count = props.count ? props.count as int : 10
        // get twitter object

        Paging paging = null


        if (props.page && props.sinceId && props.count && props.sinceId && props.maxId) {

            paging = new Paging(props.page as int, props.count as int, props.sinceId as long, props.maxId as long)

        } else if (props.page && props.count && props.sinceId) {

            paging = new Paging(props.page as int, props.count as int, props.sinceId as long)

        } else if (props.page && props.count) {
            paging = new Paging(props.page as int, props.count as int)

        } else if (props.page && props.sinceId) {
            paging = new Paging(props.page as int, props.sinceId as long)

        } else if (props.page) {
            paging = new Paging(props.page as int)

        } else if (props.sinceId) {
            paging = new Paging(props.sinceId as long)

        } else {
            paging = new Paging()

        }

        // initialize twitter object
        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)


        List<Status> tweetList = [] // new empty list
        List<Status> result = []
        long sinceId = 0;
        int index = props.page ? props.page as int : 0
        int remaining = 0;
        if (props.screenName) {
            //priming read
            result = twitter.getUserTimeline(props.screenName, paging)
            // add result to tweetList

            while ((tweetList.size() <= count) && !result.isEmpty()) {

                tweetList += result// add result to tweets list


                result.sort({ it.id })//sort the result by tweet id

                sinceId = Math.min(result.get(0).id, result.get(result.size() - 1).id)
// take the smallest tweet id from the list of tweets

                index++// increment page index

                paging.setPage(index)// update paging object
                paging.setMaxId(sinceId)
// set the lowest tweet id as the maximum tweet id in the next round of retrieval


                remaining = count - tweetList.size()// set count to the remaining number of tweets
                if (remaining <= 0) {
                    break;
                }
                paging.setCount(remaining)

                result = twitter.getUserTimeline(props.screenName, paging)// retrieve more ...

            }

        } else if (props.userId) {
            //priming read
            result = twitter.getUserTimeline(props.userId, paging)
            // add result to tweetList
            tweetList += result
            while ((tweetList.size() < count) && !result.isEmpty()) {
                tweetList += result// add result to tweets list


                result.sort({ it.id })//sort the result by tweet id

                sinceId = Math.min(result.get(0).id, result.get(result.size() - 1).id)
// take the smallest tweet id from the list of tweets

                index++// increment page index

                paging.setPage(index)// update paging object
                paging.setMaxId(sinceId)
// set the lowest tweet id as the maximum tweet id in the next round of retrieval


                remaining = count - tweetList.size()// set count to the remaining number of tweets
                if (remaining <= 0) {
                    break;
                }
                paging.setCount(remaining)

                result = twitter.getUserTimeline(props.userId, paging)// retrieve more ...
            }

        } else {
            tweetList = twitter.getUserTimeline()
        }

        return formatStatus(tweetList)

    }

    /**
     * get statuses ( tweets ) where the authenticated user was mentioned
     * @param props.page : page number
     * @param props.count : number of statuses
     * @param props.sinceId : status id to start from
     * @param props.maxId : status id to stop at
     * @param twitterMap : configured twitter object
     * */
    List<Map> getMentionsTimeline(Map props, Map twitterMap) {

        //Paging(int page, int count, long sinceId, long maxId) 
        Paging paging = null


        if (props.page && props.sinceId && props.count && props.sinceId && props.maxId) {

            paging = new Paging(props.page as int, props.count as int, props.sinceId as long, props.maxId as long)

        } else if (props.page && props.count && props.sinceId) {

            paging = new Paging(props.page as int, props.count as int, props.sinceId as long)

        } else if (props.page && props.count) {
            paging = new Paging(props.page as int, props.count as int)

        } else if (props.page && props.sinceId) {
            paging = new Paging(props.page as int, props.sinceId as long)

        } else if (props.page) {
            paging = new Paging(props.page as int)

        } else if (props.sinceId) {
            paging = new Paging(props.sinceId as long)

        } else {
            paging = new Paging()

        }

        // initialize twitter object
        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)

        List<Status> tweetList = [] // new empty list
        List<Status> result = []
        long sinceId = 0;
        int index = props.page ? props.page as int : 0
        result = twitter.getMentionsTimeline(paging)
        // add result to tweetList
        tweetList += result
        while (result) {
            //sort the result by tweet id
            result.sort({ it.id })

            // increment page index
            index++
            // update paging object
            paging.setPage(index)
            // pick the last element id
            sinceId = result.get(result.size() - 1).id
            // update the paging object
//                paging.setSinceId(sinceId)
            paging.setMaxId(result.get(0).id)
            // retrieve more ...
            result = twitter.getUserTimeline(props.screenName, paging)

            // add result to tweetList
            tweetList += result

            // increment page index

        }

        return formatStatus(tweetList)

    }

    /**
     *
     * @param twitterMap: credentials
     * @param count: maximum number of tweets to retrieve. Maximum is 800, as per Twitter API
     * @param sinceId: All tweets returned will have Id greater than this value
     * @return
     */
    List<Map> getMentionsTimeline(Map twitterMap, int count = 10, long sinceId = 1, Long maxId = null){
        Paging paging = new Paging(count: Math.min(count, 800), sinceId: Math.max(sinceId, 1))

        if(maxId){
            paging.maxId = maxId
        }

        // initialize twitter object
        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)
        List<Status> tweetList = twitter.getMentionsTimeline(paging)
        formatStatus(tweetList)
    }


    /**
     *
     * @param twitterMap: credentials
     * @param count: maximum number of tweets to retrieve. Maximum is 800, as per Twitter API
     * @param sinceId: All tweets returned will have Id greater than this value
     * @return
     */
    List<Map> getDirectMessages(Map twitterMap, int count = 10, Long sinceId = null, Long maxId = null){
        Paging paging = new Paging(count: Math.min(count, 800))

        if(sinceId > 0){
            paging.sinceId = sinceId
        }

        if(maxId > 0){
            paging.maxId = maxId
        }

        // initialize twitter object
        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)
        List<DirectMessage> messageList = twitter.getDirectMessages(paging)
        formatDirectMessage(messageList)
    }

    /**
     * Used to send direct messages to a user  
     * @param props.recipientId : user id for the recipient
     * @param props.text : body of the tweet
     * @param twitterMap : configured twitter object
     * */
    Map sendDirectMessage(Map props, Map twitterMap) {

        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)
        DirectMessage message = null
        if (props.recipientId) {
            message = twitter.sendDirectMessage(props.recipientId as long, props.text);
        } else if (props.screenName) {
            message = twitter.sendDirectMessage(props.screenName, props.text);
        } else {
            message = null
        }


        return formatDirectMessage(message)

    }

    /**
     * Used to retrieve direct messages for a user 
     * @param props.page : page number
     * @param props.count : number of direct messages
     * @param props.sinceId : directMessage id to start from
     * @param props.maxId : direct message id to stop at
     * @param twitterMap : configured twitter object
     * */
    List<Map> getDirectMessages(Map props, Map twitterMap) {

        Paging paging = null
        int page = props.page ? props.page as int : 1
        int count = props.count ? props.count as int : 30
        int sinceId = props.sinceId ? props.sinceId as long : 1
        if (props.maxId) {
            paging = new Paging(page, count, sinceId, props.maxId as long)
        } else {
            paging = new Paging(page, count, sinceId)
        }
        // get twitter object
        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)

        List<DirectMessage> result = twitter.getDirectMessages(paging)

        return formatDirectMessage(result)

    }

    /**
     * Used to retrieve sent direct messages for a user 
     * @param props.page : page number
     * @param props.count : number of direct messages
     * @param props.sinceId : directMessage id to start from
     * @param props.maxId : direct message id to stop at
     * @param twitterMap : configured twitter object
     * */
    List<Map> getSentDirectMessages(Map props, Map twitterMap) {

        Paging paging = null
        int page = props.page ? props.page as int : 1
        int count = props.count ? props.count as int : 30
        int sinceId = props.sinceId ? props.sinceId as long : 1
        if (props.maxId) {
            paging = new Paging(page, count, sinceId, props.maxId as long)
        } else {
            paging = new Paging(page, count, sinceId)
        }

        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)

        List<DirectMessage> result = twitter.getSentDirectMessages(paging)

        return formatDirectMessage(result)

    }

    /**
     * Used to retrieve a direct message for a user 
     * @param props.id : direct message id
     * @param twitterMap : configured twitter object
     * */
    Map showDirectMessage(Map props, Map twitterMap) {

        int id = props.id as long

        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)

        DirectMessage result = twitter.showDirectMessage(id)

        return formatDirectMessage(result)

    }

    /**
     * Used to delete a direct message for a user 
     * @param props.id : direct message id
     * @param twitterMap : configured twitter object
     * */
    Map destroyDirectMessage(Map props, Map twitterMap) {
        //Paging(int page, int count, long sinceId, long maxId) 
        int id = props.id as long

        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)

        DirectMessage result = twitter.destroyDirectMessage(id)

        return formatDirectMessage(result)

    }

    /**
     * Used to retrieve a status(tweet) for a user 
     * @param props.statusId : direct message id
     * @param twitterMap : configured twitter object
     * */
    Map showStatus(Map props, Map twitterMap) {
        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)
        return formatStatus(twitter.showStatus(new Long(props.statusId)))
    }

    /**
     * Used to format twitter4j statuses as Map 
     * with added string ids (idStr) for properties
     * @param statuses : List of statuses
     * @returns List < Map >  a list of maps
     *
     * */
    List<Map> formatStatus(List<Status> statuses) {
        List list = []
        for (Status status : statuses) {
            list.add(formatStatus(status))
        }
        return list
    }

    /**
     * Used to format twitter4j status as Map 
     * with added string ids (idStr) for properties
     * @param status : status object
     * @returns Map: a map object that contains properties of status
     *
     * */
    Map formatStatus(Status status) {
        Map map = [:]
        map += status.getProperties() as Map
        map['idStr'] = "${map.id}" // add string id

        map['inReplyToUserIdStr'] = "${map?.inReplyToUserId}"

        map['inReplyToStatusIdStr'] = "${map?.inReplyToStatusId}"

        map['userId'] = "${map?.user.id}"
        map['screenName'] = "${map?.user.screenName}"
        map['profileImageUrl'] = "${map?.user.profileImageUrl}"
        map.user = status.user.getProperties() as Map
        map.mediaEntities = status.mediaEntities.getProperties() as Map
        map.hashtagEntities = status.hashtagEntities.getProperties() as Map
        map.userMentionEntities = status.userMentionEntities.getProperties() as Map
        map.extendedMediaEntities = status.mediaEntities.getProperties() as Map
        map.retweetedStatus = status.retweetedStatus.getProperties() as Map

        return map

    }

    /**
     * Used to format twitter4j direct message as Map 
     * with added string ids (idStr) for properties
     * @param directMessage : directMessage object
     * @returns Map: a map object that contains properties of direct messages
     *
     * */
    Map formatDirectMessage(DirectMessage directMessage) {

        Map map = [:]
        map += directMessage.getProperties() as Map
        map['idStr'] = "${map.id}" // add string id

        map['senderId'] = "${map?.senderId}"

        map['recipientId'] = "${map?.recipientId}"

        map['senderName'] = "${map?.sender.name}"

        return map

    }

    /**
     * Used to format twitter4j direct messages as Map 
     * with added string ids (idStr) for properties
     * @param dms : list of direct message objects
     * @returns List < Map > : a list of map object that contains properties of direct messages
     *
     * */
    List<Map> formatDirectMessage(ResponseList<DirectMessage> dms) {
        List list = []
        for (DirectMessage dm : dms) {
            list.add(formatDirectMessage(dm))
        }
        return list
    }

    /**
     * Used to format twitter4j rate limit as Map 
     * with added string ids (idStr) for properties
     * @param status : status object
     * @returns Map: a map object that contains properties of status
     *
     * */
    Map formatRateLimit() {


    }

    /**
     * Used to post a tweet to twitter 
     * @param statusId : tweet to which this is a reply
     * @param text : body of the tweet
     * @param twitter : configured twitter object
     * */
    Map updateStatus(Map props, Map twitterMap) {

        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)

        Status update = null;
        StatusUpdate statusUpdate = null
        if (props.statusId) {// if this is reply to a status(tweet)
            statusUpdate = new StatusUpdate(props.text).inReplyToStatusId(props.statusId as long)
        } else {
            statusUpdate = new StatusUpdate(props.text)//.inReplyToStatusId(statusId as long) 
        }
        update = twitter.updateStatus(statusUpdate);

        return formatStatus(update)

    }

    /**
     * Used to delete a tweet from twitter
     * @param props.statusId : tweet to destroy
     * */
    Map destroyStatus(Map props, Map twitterMap) {

        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken, twitterMap.accessTokenSecret)

        Status status = twitter.destroyStatus(props.statusId as long);
        return formatStatus(status)


    }

    /**
     * Get basic authentication for making twitter api calls 
     * @param consumerKey : app consumer key
     * @param consumerSecret : app consumer secret
     *
     * */
    String getBasicAuthToken(String consumerKey, String consumerSecret) {
        def bConsumerKey = consumerKey
        def bConsumerSecret = consumerSecret
        // according to twitter guidelines
        return "${bConsumerKey}:${bConsumerSecret}".bytes.encodeBase64().toString()
    }

    /**
     * Get bearer token required to make twitter api calls 
     * @param consumerKey : app consumer key
     * @param consumerSecret : app consumer secret
     *
     * */
    private String getBearerToken(String consumerKey, String consumerSecret) {
        // get basic authentication
        String basic = getBasicAuthToken(consumerKey, consumerSecret)
        String bearer
        HTTPBuilder httpBuilder = new HTTPBuilder(getOAuthUrl())
        httpBuilder.handler.failure = { resp, json ->
            println json
        }
        // set the Authorization header for the request
        httpBuilder.setHeaders("Authorization": "Basic $basic")

        try {// make post request to twitter
            httpBuilder.post(body: [grant_type: "client_credentials"]) { resp, json ->

                bearer = json.access_token
            }
            return bearer
        }
        catch (HttpResponseException e) {
            println e.message
            println e.getResponse().data
        }
    }

    /**
     * Build a header string using twitter algorithm
     * @param props
     * */
    String buildHeaderString(Map props) {
        //        Append the string “OAuth ” (including the space at the end) to DST.
        def keySet = props.keySet()

        String dst = "";
        dst += "OAuth "
        for (int i = 0; i < keySet.size(); i++) {
            dst += encode(keySet[i])
            dst += "="
            dst += '"'
            dst += encode("${props[keySet[i]]}")
            dst += '"'
            // if there are more items in the map, append a comma and space ' '
            if (i + 1 < keySet.size()) {
                dst += ","
                dst += " "
            }
        }

        return dst
    }

    /**
     * generate authorization header string using twitter algorithm
     * @param httpMethod : GET or POST depending on kind of request you want to make
     * @param url : the request url
     * @param requestParameters : the url parameters of the request
     * @param consumerKey : twitter consumer key
     * @param consumerSecret : twitter consumer secret
     * @oAuthToken : oAuth token
     * @oAuthTokenSecret : oAuth token secret
     * */
    private String generateAuthorizationHeader(String httpMethod, String url,
                                               Map requestParams, String consumerKey, String consumerSecret,
                                               String oAuthToken, String oAuthTokenSecret) {
        // create a map of the following ... as specified by twitter
        Map oAuthParams = [
                'oauth_version'         : getOAuthVersion(),
                'oauth_consumer_key'    : consumerKey,
                'oauth_nonce'           : getOAuthNonce(),

                'oauth_signature_method': getOAuthSignatureMethod(),
                'oauth_timestamp'       : "${getTimeStamp()}",
                'oauth_token'           : oAuthToken
        ]

        Map allParams = oAuthParams + requestParams + getUrlParameters(url)
        // add all the pams together
        int queryStart = url.indexOf("?");

        // extract base url of the request
        String baseUrl = ""
        if (queryStart != -1) {
            baseUrl = url.substring(0, queryStart)
        } else {
            baseUrl = url
        }
        // get oAuth signature .... using specified algorithm 
        String oAuthSignature = getOAuthSignature(httpMethod, baseUrl, allParams, consumerSecret, oAuthTokenSecret)
        oAuthParams.put('oauth_signature', oAuthSignature)

        return buildHeaderString(oAuthParams)

    }

    /**
     * get oAuth signature using specified algorithm
     * @param httpMethod : GET or POST depending on kind of request you want to make
     * @param baseUrl : the request base url
     * @param allParams : all the parameters of the request
     * @param consumerSecret : twitter consumer secret
     * @oAuthTokenSecret : oAuth token secret
     * */
    String getOAuthSignature(String httpMethod, String baseUrl, Map allParams, String consumerSecret, String oAuthTokenSecret) {
        //Percent encode every key and value that will be signed.
        Map encodedMap = [:]
        allParams.each { key, value ->
            encodedMap.put(encode(key), encode("${value}"))
        }

        //Sort the list of parameters alphabetically[1] by encoded key[2].
        encodedMap.sort()
        //For each key/value pair:
        String outputString = ""
        int i = encodedMap.size() // take the size
        encodedMap.each { key, value ->
            i--;
            // Append the encoded key to the output string.
            outputString += key
            //Append the ‘=’ character to the output string.
            outputString += '='
            //Append the encoded value to the output string.
            outputString += value
            if (i > 0) {
                outputString += '&'
            }
        }
        String signatureBaseString = getSignatureBaseString(httpMethod, baseUrl, outputString)
        generateSignature(signatureBaseString, consumerSecret, oAuthTokenSecret)
    }

    /**
     * generate signature base string using twitter algorithm
     * @param httpMethod : GET or POST depending on kind of request you want to make
     * @param baseUrl : the request url
     * @param outputString : string from oauth signature stepa
     *
     * */
    String getSignatureBaseString(String httpMethod, String baseUrl, String outputString) {
        //        Creating the signature base string
        String signatureBaseString = ""
        //        Convert the HTTP Method to uppercase and set the signatureBaseString string equal to this value.
        signatureBaseString += httpMethod.toUpperCase()
        //        Append the ‘&’ character to the output string
        signatureBaseString += '&'
        //        Percent encode the URL and append it to the output string.
        signatureBaseString += encode(baseUrl)
        //        Append the ‘&’ character to the output string
        signatureBaseString += '&'
        //        Percent encode the parameter string and append it to the signatureBaseString string.
        signatureBaseString += encode(outputString)

        return signatureBaseString
    }

    /**
     * percentage encoding
     *
     * @return String: encoded string
     */
    private String encode(String value) {
        String encoded = "";
        try {
            encoded = URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        String sb = "";
        char focus;
        for (int i = 0; i < encoded.length(); i++) {
            focus = encoded.charAt(i);
            if (focus == '*') {
                sb += "%2A";
            } else if (focus == '+') {
                sb += "%20";
            } else if (focus == '%' && i + 1 < encoded.length()
                    && encoded.charAt(i + 1) == '7' && encoded.charAt(i + 2) == 'E') {
                sb += '~';
                i += 2;
            } else {
                sb += focus;
            }
        }
        return sb.toString();
    }

    def getOAuthToken() {

    }

    /**
     * generate a timestamp
     * returns long : 
     * */
    long getTimeStamp() {
        Calendar cal = Calendar.getInstance()
        TimeZone gmtTime = TimeZone.getTimeZone("GMT");
        print cal.getTime()
        cal.setTimeZone(gmtTime)
        print cal.getTime()
        return cal.getTimeInMillis()
    }

    private Date cvtToGmt(Date date) {
        TimeZone tz = TimeZone.getDefault();
        Date ret = new Date(date.getTime() - tz.getRawOffset());

        // if we are now in DST, back off by the delta.  Note that we are checking the GMT date, this is the KEY.
        if (tz.inDaylightTime(ret)) {
            Date dstDate = new Date(ret.getTime() - tz.getDSTSavings());

            // check to make sure we have not crossed back into standard time
            // this happens when we are on the cusp of DST (7pm the day before the change for PDT)
            if (tz.inDaylightTime(dstDate)) {
                ret = dstDate;
            }
        }
        return ret;
    }

    String getOAuthSignatureMethod() {
        return "HMAC-SHA1"
    }

    /**
     * generate random string
     * */
    String getOAuthNonce() {


        int numberOfGroups = 3;
        int sizePerGroup = 4;
        SecureRandom random = new SecureRandom();

        String characters = "ABCDEFGHJKLMNPQRTUVWXY";// space
        //random.ints(1,0, k.size()).toArray()
        String rand = "";
        for (int i = 0; i < numberOfGroups * sizePerGroup; i++) {
            rand += characters[random.ints(1, 0, characters.size()).toArray()[0]]

        }

        return rand.toLowerCase()
    }

    String getOAuthVersion() {
        return "1.0"
    }


    Map getUrlParameters(String url) {
        //        int queryStart = url.indexOf("?");
        //        if (-1 != queryStart) {
        //            url.split("&");
        //            String[] queryStrs = url.substring(queryStart + 1).split("&");
        //            try {
        //                for (String query : queryStrs) {
        //                    String[] split = query.split("=");
        //                    if (split.length == 2) {
        //                        signatureBaseParams.add(
        //                            new HttpParameter(URLDecoder.decode(split[0],
        //                                        "UTF-8"), URLDecoder.decode(split[1],
        //                                        "UTF-8"))
        //                        );
        //                    } else {
        //                        signatureBaseParams.add(
        //                            new HttpParameter(URLDecoder.decode(split[0],
        //                                        "UTF-8"), "")
        //                        );
        //                    }
        //                }
        //            } catch (UnsupportedEncodingException ignore) {
        //            }
        //
        //        }
        return [:]
    }

    /**
     * Generate signature string for twitter request
     * @param signatureBaseStr : signature base String
     * @param oAuthConsumerSecret : OauthConsumer secret
     * @param oAuthTokenSecret : oauth token secret
     *
     * */
    private String generateSignature(String signatueBaseStr, String oAuthConsumerSecret,
                                     String oAuthTokenSecret) {
        byte[] byteHMAC = null;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec spec;
            if (null == oAuthTokenSecret) {
                String signingKey = encode(oAuthConsumerSecret) + '&';
                spec = new SecretKeySpec(signingKey.getBytes(), "HmacSHA1");
            } else {
                String signingKey = encode(oAuthConsumerSecret) + '&' + encode(oAuthTokenSecret);
                spec = new SecretKeySpec(signingKey.getBytes(), "HmacSHA1");
            }
            mac.init(spec);
            byteHMAC = mac.doFinal(signatueBaseStr.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new BASE64Encoder().encode(byteHMAC);
    }

    /**
     * Send a get request to given uri
     * @param uri eg "https://api.twitter.com/1.1/statuses/"
     * @param path : eg "user_timeline.json
     * @param twitter : map of twitter settings
     * @param queryMap : map of query parameters
     * */
    def get(String uri, String path, Map twitter, Map queryMap) {

        HTTPBuilder restClient = new HTTPBuilder(uri)

        def bearerToken = getBearerToken(twitter.consumer_key, twitter.consumer_secret)
        restClient.handler.failure = { resp, json ->
            println resp.statusLine
            println json
            //bearerToken = getBearerToken()
            if (resp.status == 429) {
                println(resp.headers.'X-Rate-Limit-Reset')
                System.exit(1)
            }
        }

        restClient.setHeaders("Authorization": "Bearer ${bearerToken}")


        restClient.get(path: path, query: queryMap) { resp, json ->

            return json

        }


    }

    /**
     * Send a post request to given uri
     * @param uri eg "https://api.twitter.com/"
     * @param path : eg "oauth2/token"
     * @param twitter : map of twitter settings
     * @param queryMap : map of query parameters
     * */
    String post(String uri, String path, Map twitter, Map reqBody) {
        String httpMethod = "POST"
        String url = uri + path
        HTTPBuilder restClient = new HTTPBuilder(uri + path)

        String authorizationHeader = generateAuthorizationHeader(httpMethod, url, reqBody, twitter.consumerKey, twitter.consumerSecret, twitter.oAuthToken, twitter.oAuthTokenSecret)
        //getBearerToken(twitter.consumer_key,twitter.consumer_secret)
        restClient.handler.failure = { resp, json ->
            println resp.statusLine
            println json

            if (resp.status == 429) {
                println(resp.headers.'X-Rate-Limit-Reset')
                System.exit(1)
            }
        }

        restClient.setHeaders("Authorization": "${authorizationHeader}")


        restClient.post(body: reqBody) { resp, json ->

            return json

        }


    }

    public HttpResponse post(String path, Map twitterMap, Map reqBody)
            throws TwitterException {
        def request = composeRequest(path, twitterMap, reqBody)
        HttpClient client = request.httpClient
        return client.post(request.url, request.params, request.auth, request.listener)
    }

    public HttpResponse put(String path, Map twitterMap, Map reqBody)
            throws TwitterException {
        def request = composeRequest(path, twitterMap, reqBody)
        HttpClient client = request.httpClient
        return client.put(request.url, request.params, request.auth, request.listener)
    }

    public HttpResponse get(String path, Map twitterMap, Map reqBody)
            throws TwitterException {
        def request = composeRequest(path, twitterMap, reqBody)
        HttpClient client = request.httpClient
        return client.get(request.url, request.params, request.auth, request.listener)
    }

    public HttpResponse delete(String path, Map twitterMap, Map reqBody)
            throws TwitterException {
        def request = composeRequest(path, twitterMap, reqBody)
        HttpClient client = request.httpClient
        return client.delete(request.url, request.params, request.auth, request.listener)
    }

    private Map composeRequest(String path, Map twitterMap, Map reqBody){
        Twitter twitter = getTwitter(twitterMap.consumerKey, twitterMap.consumerSecret, twitterMap.accessToken,
                twitterMap.accessTokenSecret, twitterMap.isApplicationOnlyAuth ? twitterMap.isApplicationOnlyAuth : false)
        def auth = twitter.getAuthorization()

        if (!auth.isEnabled()) {
            throw new IllegalStateException(
                    "Authentication credentials are missing. " + WWW_DETAILS);
        }

        def conf = twitter.getConfiguration()
        def client = HttpClientFactory.getInstance(conf.getHttpClientConfiguration());
        def url = conf.getRestBaseURL() + path

        List<HttpParameter> params = new ArrayList<HttpParameter>(reqBody.size() as int);
        reqBody.each {
            params.add(new HttpParameter(it.key as String, it.value as String))
        }
        [ httpClient: client, url: url, params: params.toArray(new HttpParameter[0]), auth: auth, listener: null ]
    }

    /**
     * Creates a new webhook for the application
     * @param props.url : The callback URL
     */
    public JSONObject createWebhook(Map props, Map twitterMap) {
        def response = post(URL_CREATE_WEBHOOK_FMT, twitterMap, props)
        convertToJSONObject(response)
    }

    /**
     * Updates the webhook url, useful to revalidate the webhook if it fails the
     * Twitter daily validity check
     * @param props: props.id {ID of the webhook to update}
     */
    public JSONObject updateWebhook(Map props, Map twitterMap) {
        String formattedUrl = sprintf(URL_UPDATE_WEBHOOK_FMT, props.id)
        def response = put(formattedUrl, twitterMap, [:])
        convertToJSONObject(response)
    }

    /**
     * Fetches the webhook belonging to the application with the provided credentials
     * @param twitterMap : A map containing the Twitter OAuth credentials
     */
    public JSONArray getWebhook(Map twitterMap) {
        def response = get(URL_GET_WEBHOOK_FMT, twitterMap, [:])
        convertToJSONArray(response)
    }

    /**
     * Delete a webhook for an application with the credentials
     * @param twitterMap: credentials
     * @param props: props.id {ID of the webhook to delete}
     * @return
     */
    def deleteWebhook(Map props, Map twitterMap){
        String formattedUrl = sprintf(URL_DELETE_WEBHOOK_FMT, props.id)
        def response = delete(formattedUrl, twitterMap, [:])
        return response
    }

    /**
     * Subscribe the authenticating user to the webhook
     * @param props.url
     * Expected response 204: Subscription added for provided user.
     */
    def subscribeToWebhook(Map props, Map twitterMap){
        def response = post(URL_SUBSCRIBE_TO_WEBHOOK_FMT, twitterMap, props)
        return response
    }

    /**
     * Get the subscription of the authenticating user to the webhook with the supplied webhookId
     * @param props.webhookId
     * Expected response 204: Subscription removed for provided user.
     */
    def webhookUnsubscribe(Map twitterMap){
        def response = delete(URL_UNSUBSCRIBE_FROM_WEBHOOK_FMT, twitterMap, [:])
        return response
    }

    def getWebhookSubscription(Map props, Map twitterMap){
        def response = get(URL_GET_WEBHOOK_ID_FMT, twitterMap, [:])
        return response
    }

    def getWebhookStatus(Map twitterMap) {
        def response = get(URL_GET_WEBHOOK_SUBSCRIPTION_FMT, twitterMap, [:])
        return response
    }

    /**
     * Retrieve the list of active subscriptions to the webhook via the All Activities API
     * @param props.wehbookId
     * @param twitterMap
     */
    def getWebhookSubscriptionsList(Map props, Map twitterMap) {
        def subscriptionsListResponse = get(URL_GET_WEBHOOK_SUBSCRIPTION_FMT, twitterMap, [:])
        return subscriptionsListResponse
    }

    JSONObject convertToJSONObject(HttpResponse value) {
        def response = JSON.parse(value.asString())
        response as JSONObject
    }

    JSONArray convertToJSONArray(HttpResponse value) {
        def response = JSON.parse(value.asString())
        response as JSONArray
    }
}
