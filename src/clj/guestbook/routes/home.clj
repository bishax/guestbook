(ns guestbook.routes.home
  (:require
   [guestbook.layout :as layout]
   [guestbook.db.core :as db]
   [clojure.java.io :as io]
   [guestbook.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [struct.core :as st]))

(defn home-page [request]
  (layout/render request "home.html" (merge {:messages (db/get-messages)})))

(defn about-page [request]
  (layout/render request "about.html"))

(def message-schema
  [[:name
    st/required
    st/string]
   [:message
    st/required
    st/string
    {:message "message must contain at least 10 characters"
     :validate (fn [msg] (>= (count msg) 10))}]])

(defn validate-message [params]
  (first (st/validate params message-schema)))

(defn save-message! [{:keys [params]}]
  (if-let [errors (validate-message params)]
    (response/bad-request {:errors errors})
    (try
        (db/save-message! params) ; add message to DB
        (response/ok {:status :ok})
        (catch Exception e
          (response/internal-server-error
           {:errors {:server-error ["Failed to save message!"]}})))))


(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/about" {:get about-page}]
   ["/message" {:post save-message!}]])
