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
  "get table column info"
  [tables]
  (select pg_columns
          (fields :table_name
                  :column_name
                  :data_type)
          (where {:table_name [in tables]})
          (order :table_name :ASC)
          (order :ordinal_position :ASC)))

(defn get-table-column-map
  "create a column info map with table name key"
  [ini-settings]
  (let [table-cols (get-postgresql-table-columns (str/split (:tables (:db ini-settings)) #","))]
    (reduce (fn [m col]
              (assoc m
                     (keyword (:table_name col))
                     (conj (get m (keyword (:table_name col)) []) col))) {} table-cols)))

(defn create-column-type-map
  "create a column type map with column name key"
  [table-column-list]
  (reduce (fn [m col]
            (assoc m
                   (keyword (:column_name col))
                   (:data_type col))) {} table-column-list))

(defn cast-columns
  "add casts according to column data type"
  [column-type-map row-map]
  (into {} (for [[col-name data] row-map]
             [col-name (cond
                         (= (get column-type-map (keyword col-name)) "jsonb") (raw (str "'" data "'::jsonb"))
                         :else data)])))

(defn export-table-info
  "export table and column names to Excel file"
  [ini-settings]
  (let [col-map (get-table-column-map ini-settings)
        wb (ss/create-workbook "Sheet1" [])]
    (doseq [[table-name cols] col-map]
      (-> (ss/add-sheet! wb (name table-name))
        (ss/add-row! (map :column_name cols)))
      (ss/save-workbook! (:export-path (:xlsx ini-settings)) wb))))

(defn clean-import-tables
  "import table data from Excel file"
  [ini-settings]
  (let [col-map (get-table-column-map ini-settings)
        wb (ss/load-workbook (:import-path (:xlsx ini-settings)))]
    (doseq [[table-name cols] col-map]
      (let [column-type-map (create-column-type-map cols)
            sheet (ss/select-sheet (name table-name) wb)
            rows (ss/row-seq sheet)
            header-row (map #(keyword (ss/read-cell %)) (first rows))
            data-rows (rest rows)]
        (delete table-name)
        (cond
          (> (count rows) 1) (insert table-name
                                     (values (for [row data-rows] (cast-columns column-type-map (zipmap header-row (map #(ss/read-cell %) row)))))))
        (println (str "inserted: " table-name))))))

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
       :subprotocol (:dbprotocol db-settings)})
    (cond
      (= command "export-table-info") (export-table-info ini-settings)
      (= command "import-table") (clean-import-tables ini-settings))))

