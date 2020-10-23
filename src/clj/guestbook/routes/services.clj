(ns guestbook.routes.services
  (:require
   [guestbook.layout :as layout]
   [clojure.java.io :as io]
   [guestbook.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [guestbook.messages :as msg]))


(defn service-routes []
  ["/api"
   {:middleware [middleware/wrap-formats]}
   ["/api/messages" {:get
                 (fn [_]
                   (response/ok (msg/message-list)))}]
   ["/api/message" {:post
                (fn [{:keys [params]}]
                  (try
                    (msg/save-message! params)
                    (response/ok {:status :ok})
                    (catch Exception e
                      (let [{id :guestbook/error-id
                             errors :errors} (ex-data e)]
                        (case id
                          :validation
                          (response/bad-request {:errors errors})
                          ;; else
                          (response/internal-server-error
                           {:errors {:server-error ["Failed to save message!"]}}))))))}]])
