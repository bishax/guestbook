(ns guestbook.auth.ring
  (:require
   [clojure.tools.logging :as log]
   [guestbook.auth :as auth]
   [reitit.ring :as ring]))

(defn authorised? [roles req]
  (if (seq roles)
    (->> req
         :session
         :identity
         auth/identity->roles
         (some roles)
         boolean)
    (do
      (log/error "roles: " roles "is empty for route: " (:uri req))
      false)))

(defn get-roles-from-match
  "Get roles required for `req`"
  [req]
  (-> req
      (ring/get-match)
      (get-in [:data ::auth/roles] #{})))

(defn wrap-authorised [handler unauthorised-handler]
  (fn [req]
    (if (authorised? (get-roles-from-match req) req)
      (handler req)
      (unauthorised-handler req))))

(comment

  (ring/get-match {:uri "/api/messages"}))
