(defproject pgrest "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]
                 [http-kit "2.1.19"]
                 [route-map "0.0.2"]
                 [clj-time "0.11.0"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.postgresql/postgresql "9.4-1205-jdbc41"]
                 [com.zaxxer/HikariCP "2.4.2"]
                 [honeysql "0.6.2"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [circleci/clj-yaml "0.5.5"]
                 [org.clojure/data.csv "0.1.3"]
                 [environ "1.0.1"]
                 [org.clojure/tools.reader "1.0.0-alpha1"]
                 [ring/ring-defaults "0.1.5"]]
  :source-paths ["src/clj" "clj-pg/src" "clj-pg/test"]
  :plugins [[lein-ancient "0.6.4"]]
  :ring {:handler pgrest.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
