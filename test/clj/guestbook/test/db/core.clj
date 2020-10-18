(ns guestbook.test.db.core
  (:require
   [guestbook.db.core :refer [*db*] :as db]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [clojure.java.jdbc :as jdbc]
   [guestbook.config :refer [env]]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'guestbook.config/env
     #'guestbook.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-guestbook
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    ; Test save
    (is (= 1 (db/save-message!
              t-conn
              {:name "Bob"
               :message "Hello, world!"}
              {:connection t-conn})))
    ; Test get-messages
    (is (= {:name "Bob"
            :message "Hello, world!"}
           (-> (db/get-messages t-conn {})
               (first)
               (select-keys [:name :message]))))))
