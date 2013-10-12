(defproject smee "0.0.1-SNAPSHOT"
  :description "An async hook/callback library for Clojure"
  :url "https://www.github.com/leathekd/smee"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [org.clojure/tools.logging "0.2.6"]]
  :profiles {:dev {:dependencies [[log4j "1.2.17"]
                                  [org.clojure/clojure "1.5.1"]]}}
  :min-lein-version "2.0.0"
  :plugins [[lein-bikeshed "0.1.3"]])
