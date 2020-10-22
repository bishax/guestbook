(ns guestbook.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [re-frame.core :as rf]
            [ajax.core :refer [GET POST]]
            [clojure.string :as string]
            [guestbook.validation :refer [validate-message]]))


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

(rf/reg-event-db
 ; "DB event for adding new message"
 :message/add
 (fn [db [_ message]]
   (update db :messages/list conj message)))

(defn send-message! [fields errors]
  (if-let [validation-errors (validate-message @fields)]
    (reset! errors validation-errors)
    (POST "/message"
        {:format :json
         :headers {"Accept" "application/transit+json"
                   "x-csrf-token" (.-value (.getElementById js/document "token"))}
         :params @fields
         :handler #(do
                     (rf/dispatch [:message/add (assoc @fields :timestamp (js/Date.))])
                     (reset! fields nil)
                     (reset! errors nil))
         :error-handler #(do
                           (.log js/console (str %))
                           (reset! errors (get-in % [:response :errors])))})))

(defn errors-component [errors id]
  (when-let [error (id @errors)]
        [:div.notification.is-danger (string/join error)]))

(rf/reg-event-db
 ;"DB event for getting messages"
 :messages/set
 (fn [db [_ messages]]
   (-> db
       (assoc :messages/loading? false
              :messages/list messages))))

(rf/reg-sub
 ;"Subscription for getting messages"
 :messages/list
 (fn [db _]
   (:messages/list db [])))

(defn get-messages []
  (GET "/messages"
       {:headers {"Accept" "application/transit+json"}
        :handler #(rf/dispatch [:messages/set (:messages %)])}))

(defn message-list [messages]
  [:ul.messages
   (for [{:keys [timestamp message name]} @messages]
    ^{:key timestamp}
     [:li
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p " - " name]])])

(rf/reg-event-fx
 ;"Initialisation event, while loading data"
 :app/initialize
 (fn [_ _]
   {:db {:messages/loading? true}}))


(rf/reg-sub
 ;"Subscription for message loading"
 :messages/loading?
 (fn [db _]
   (:messages/loading? db)))

(defn home []
  (let [messages (rf/subscribe [:messages/list])]
    (rf/dispatch [:app/initialize])
    (get-messages)
    (fn []
      [:div.content>div.columns.is-centered>div.column.is-two-thirds
       (if @(rf/subscribe [:messages/loading?])
        [:h3 "Loading Messages..."]
        [:div
         [:div.columns>div.column
          [:h3 "Messages"]
          [message-list messages]]
         [:div.columns>div.column
          [message-form]]])])))

(dom/render
 [home]
 (.getElementById js/document "content"))
