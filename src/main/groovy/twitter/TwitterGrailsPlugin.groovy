package twitter

import grails.plugins.*

class TwitterGrailsPlugin extends Plugin {
    def grailsVersion = "3.1.1 > *"
    def title = "Twitter plugin"
    def author = "Omasirichukwu Udeinya"
    def authorEmail = "omasiri@novadge.com"
    def description = 'Integrate your app with twitter and make calls to Twitter API'
    def profiles = ['web']
    def documentation = "http://novadge.github.io/twitter/"
    def license = "APACHE"
    def organization = [name: "Novadge", url: "http://www.novadge.com/"]
    def issueManagement = [url: "https://github.com/Novadge/twitter/issues"]
    def scm = [url: "https://github.com/Novadge/twitter/"]
}
