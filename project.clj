(defproject clj-xlsx2db "0.1.0"
  :description "Application to import table data from Excel files to database tables."
  :url "https://github.com/pirosuke/clj-xlsx2db"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [dk.ative/docjure "1.9.0"]
                 [korma "0.4.2"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.postgresql/postgresql "9.4-1203-jdbc42"]
                 [clj-time "0.11.0"]
                 [clojure-ini "0.0.2"]]
  :main ^:skip-aot clj-xlsx2db.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
