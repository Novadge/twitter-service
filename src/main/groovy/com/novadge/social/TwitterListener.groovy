///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package com.novadge.social
//
///**
// *
// * @author joseph
// */
//
//import java.util.List;
//
//import twitter4j.DirectMessage;
//import twitter4j.Status;
//import twitter4j.StatusDeletionNotice;
//import twitter4j.StatusListener;
//import twitter4j.Twitter;
//import twitter4j.TwitterException;
//import twitter4j.TwitterFactory;
//import twitter4j.TwitterStream;
//import twitter4j.TwitterStreamFactory;
//import twitter4j.User;
//import twitter4j.UserList;
//import twitter4j.UserStreamListener;
//import twitter4j.auth.AccessToken;
//import twitter4j.conf.ConfigurationBuilder;
//
//public class TwitterListener {
//    private TwitterStream twitterStream;
//    private void initConfiguration(String consumerKey,String consumerSecret,String accessToken,String accessTokenSecret){
//        ConfigurationBuilder cb = new ConfigurationBuilder();
//        cb.setDebugEnabled(true)
//          .setOAuthConsumerKey(consumerKey)
//          .setOAuthConsumerSecret(consumerSecret);
//
//        TwitterStreamFactory twitterStreamFactory = new TwitterStreamFactory(cb.build());
//        twitterStream = twitterStreamFactory.getInstance(new AccessToken(accessToken, accessTokenSecret));
//        twitterStream.addListener(userStreamListener);
//        twitterStream.user();
//
//    }
//
//    UserStreamListener userStreamListener = new UserStreamListener() {
//
//        @Override
//        public void onException(Exception arg0) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onTrackLimitationNotice(int arg0) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onStatus(Status status) {
//            // TODO Auto-generated method stub
//            System.out.println(status.getText());
//        }
//
//        @Override
//        public void onScrubGeo(long arg0, long arg1) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onDeletionNotice(StatusDeletionNotice arg0) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onUserProfileUpdate(User arg0) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onUserListUpdate(User arg0, UserList arg1) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onUserListUnsubscription(User arg0, User arg1, UserList arg2) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onUserListSubscription(User arg0, User arg1, UserList arg2) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onUserListMemberDeletion(User arg0, User arg1, UserList arg2) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onUserListMemberAddition(User arg0, User arg1, UserList arg2) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onUserListDeletion(User arg0, UserList arg1) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onUserListCreation(User arg0, UserList arg1) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onUnfavorite(User arg0, User arg1, Status arg2) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onUnblock(User arg0, User arg1) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onRetweet(User arg0, User arg1, Status arg2) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onFriendList(long[] arg0) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onFollow(User arg0, User arg1) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onFavorite(User arg0, User arg1, Status arg2) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onDirectMessage(DirectMessage message) {
//            // TODO Auto-generated method stub
//            System.out.println(message.getText());
//        }
//
//        @Override
//        public void onDeletionNotice(long arg0, long arg1) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onBlock(User arg0, User arg1) {
//            // TODO Auto-generated method stub
//
//        }
//    };
//
//    public static void main(String[] args){
//        
//
//    }
//}
//
