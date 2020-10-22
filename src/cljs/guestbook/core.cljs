(ns guestbook.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [ajax.core :refer [GET POST]]
            [clojure.string :as string]))


(defn message-form []
  (let [fields (r/atom {})
        errors (r/atom nil)]
    (fn []
      [:div
       [errors-component errors :server-error]
       [:div.field
        [:label.label {:for :name} "Name"]
        [errors-component errors :name]
        [:input.input {:type :text
                       :name :name
                       :value (:name @fields)
                       :on-change #(swap! fields assoc :name (-> % .-target .-value))}]]
       [:div.field
        [:label.label {:for :message} "Message"]
        [errors-component errors :message]
        [:textarea.textarea {:name :message
                             :value (:message @fields)
                             :on-change #(swap! fields assoc :message (-> % .-target .-value))}]]
       [:input.button.is-primary
        {:type :submit
         :value "comment"
         :on-click #(send-message! fields errors)}]])))

(defn send-message! [fields errors]
  (POST "/message"
        {:format :json
         :headers {"Accept" "application/transit+json"
                   "x-csrf-token" (.-value (.getElementById js/document "token"))}
         :params @fields
         :handler #(do
                     (.log js/console (str "response:" %))
                     (reset! errors nil))
         :error-handler #(do
                           (.log js/console (str %))
                           (reset! errors (get-in % [:response :errors])))}))

(defn errors-component [errors id]
  (when-let [error (id @errors)]
        [:div.notification.is-danger (string/join error)]))

(defn home []
  [:div.content>div.columns.is-centered>div.column.is-two-thirds
    [:div.columns>div.column
        [message-form]]])

(dom/render
 [home]
 (.getElementById js/document "content"))