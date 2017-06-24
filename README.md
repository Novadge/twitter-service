Twitter service
========



<h2>Description</h2>

The Twitter service plug-in allows a grails application to use twitter API to read and write twitter data 
Eg. post tweets, retrieve tweets, send direct messages, etc.

<h2>Configuration</h2>
Add `compile 'org.grails.plugins:twitter:0.1.6'` to your build.gradle file


   
<h3>Side note </h3>
You have to retrieve twitter credentials from twitter.com in order to use their API. Go to 
[https://dev.twitter.com/] to create an app and retrieve the necessary API keys and access tokens

<h2>Usage</h2>

<em>twitterService</em> provides several methods for interactions with twitter API. 

Inject twitterService into your controller

<code>def twitterService</code>

`class MyController{    def twitterService
}`


Inside your controller action, retrieve a twitter object like this:

    
    def myCustomerAction(){    
        String consumerKey = [YOUR_TWITTER_APP_CONSUMER_KEY]
        String consumerSecret = [YOUR_TWITTER_APP_CONSUMER_SECRET]
        String accessToken = [TWITTER_ACCESS_TOKEN]
        String accessTokenSecret = [TWITTER_ACCESS_TOKEN_SECRET]
        Map twitterProps = [consumerKey:consumerKey,consumerSecret:consumerSecret,
                    accessToken:accessToken,accessTokenSecret:accessTokenSecret]
        // post a tweet
        Map twitterParams = [text:"Hello twitter"]
        twitterService.updateStatus(twitterParams, twitterProps)
        
        // delete a tweet
        twitterParams = [statusId:'THE_TWEET_ID']
        twitterService.destroyStatus(twitterParams, twitterProps)
        
        // retrieve a single tweet
        twitterParams = [statusId:'THE_TWEET_ID']
        twitterService.showStatus(twitterParams, twitterProps)
        
        // send direct message
        twitterParams = [recipientId:'TWITTER_USER_ID',text:"Hello friend"]
        twitterService.sendDirectMessage(twitterParams, twitterProps)
        
        // get direct messages
        twitterParams = [count:5]
        twitterService.getDirectMessages(twitterParams,twitterProps)
    }
`



And so much more you could do ......

<h4></h4>
Do not forget to fork the project, fix bugs, or add features :)
