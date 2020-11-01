(ns guestbook.auth.ws
  (:require
   [guestbook.auth :as auth]))

(defn authorised? [roles-by-id msg]
  (boolean
   (some (roles-by-id (:id msg) #{})
         (-> msg
             :session
             :identity
             (auth/identity->roles)))))
