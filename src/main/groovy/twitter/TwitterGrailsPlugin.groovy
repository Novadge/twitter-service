package twitter

import grails.plugins.*

class TwitterGrailsPlugin extends Plugin {
    def grailsVersion = "3.1.1 > *"
    def title = "Twitter service plugin"
    def author = "Omasirichukwu Udeinya"
    def authorEmail = "omasiri@novadge.com"
    def description = 'Use twitter API to read and write twitter data'
    def profiles = ['web']
    def documentation = "http://novadge.github.io/twitter-service/"
    def license = "APACHE"
    def organization = [name: "Novadge", url: "http://www.novadge.com/"]
    def issueManagement = [url: "https://github.com/Novadge/twitter-service/issues"]
    def scm = [url: "https://github.com/Novadge/twitter-service/"]
}
