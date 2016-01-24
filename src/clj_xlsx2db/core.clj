(ns clj-xlsx2db.core
  (:gen-class)
  (:require [clojure.string :as str])
  (:require [clj-time.coerce :as time-coerce])
  (:require [dk.ative.docjure.spreadsheet :as ss])
  (:require [clojure.pprint :as pprint])
  (:require [clojure-ini.core :as ini]))

(use 'korma.db)
(use 'korma.core)

(defentity pg_columns
  (table :information_schema.columns))

(defn get-postgresql-table-columns
  [tables]
  (select pg_columns
          (fields :table_name
                  :column_name)
          (where {:table_name [in tables]})
          (order :table_name :ASC)
          (order :ordinal_position :ASC)))

(defn get-table-column-map
  [ini-settings]
  (let [table-cols (get-postgresql-table-columns (:tables (:db ini-settings)))]
    (reduce (fn [m col]
              (assoc m
                     (keyword (:table_name col))
                     (conj (get m (keyword (:table_name col)) []) (:column_name col)))) {} table-cols)))

(defn export-tables
  [ini-settings])

(defn clean-import-tables
  [ini-settings])

(defn -main
  [& args]
  (let [command (first args)
        ini-path (second args)
        ini-settings (ini/read-ini ini-path
                                   :keywordize? true
                                   :comment-char \#)
        db-settings (:db ini-settings)]
    (defdb db
      {:user (:dbuser db-settings)
       :password (:dbpass db-settings)
       :subname (:dburl db-settings)
       :protocol (:dbprotocol db-settings)})
    (cond
      (= command "db2ss") (export-tables ini-settings)
      (= command "ss2db") (clean-import-tables ini-settings))))

