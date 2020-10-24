(ns guestbook.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [re-frame.core :as rf]
            [ajax.core :refer [GET POST]]
            [clojure.string :as string]
            [guestbook.validation :refer [validate-message]]))


(rf/reg-event-db
 ; "DB event for adding new message"
 :message/add
 (fn [db [_ message]]
   (update db :messages/list conj message)))

(rf/reg-event-fx
 :message/send!
 (fn [{:keys [db]} [_ fields]]
    (POST "/api/message"
        {:format :json
         :headers {"Accept" "application/transit+json"
                   "x-csrf-token" (.-value (.getElementById js/document "token"))}
         :params fields
         :handler #(rf/dispatch [:message/add (-> fields
                                                  (assoc :timestamp (js/Date.)))])
         :error-handler #(rf/dispatch
                          [:form/set-server-errors
                           (get-in % [:response :errors])])})
    {:db (dissoc db :form/server-errors)}))

(defn errors-component [ id]
  (when-let [error @(rf/subscribe [:form/error id])]
        [:div.notification.is-danger (string/join error)]))

(defn message-form []
    [:div
       [errors-component :server-error]
       [:div.field
        [:label.label {:for :name} "Name"]
        [errors-component :name]
        [:input.input {:type :text
                       :name :name
                       :value @(rf/subscribe [:form/field :name])
                       :on-change #(rf/dispatch [:form/set-field
                                                 :name
                                                 (.. % -target -value)])}]]
       [:div.field
        [:label.label {:for :message} "Message"]
        [errors-component :message]
        [:textarea.textarea {:name :message
                             :value @(rf/subscribe [:form/field :message])
                             :on-change #(rf/dispatch [:form/set-field
                                                       :message
                                                       (.. % -target -value)])}]]
       [:input.button.is-primary
        {:type :submit
         :disabled @(rf/subscribe [:form/validation-errors?])
         :value "comment"
         :on-click #(rf/dispatch [:message/send! @(rf/subscribe [:form/fields])])}]])

(rf/reg-event-db
 :form/set-field
 [(rf/path :form/fields)]  ; interceptor vector (similar to middleware)
 (fn [fields [_ id value]]
  (assoc fields id value)))

(rf/reg-event-db
 :form/clear-fields
 [(rf/path :form/fields)]
 (fn [_ _]
   {}))

(rf/reg-sub
 :form/fields
 (fn [db _]
   (:form/fields db)))

(rf/reg-sub
 :form/field
 :<- [:form/fields]  ; derived subscription
 (fn [fields [_ id]]
   (get fields id)))

(rf/reg-event-db
 :form/set-server-errors
 [(rf/path :form/server-errors)]
 (fn [_ [_ errors]]
   errors))

(rf/reg-sub
 :form/server-errors
 (fn [db _]
   (:form/server-errors db)))

;;Validation errors are reactively computed
(rf/reg-sub
 :form/validation-errors
 :<- [:form/fields]
 (fn [fields _]
   (validate-message fields)))

(rf/reg-sub
 :form/validation-errors?
 :<- [:form/validation-errors]
 (fn [errors _]
   (not (empty? errors))))

(rf/reg-sub
 :form/errors
 :<- [:form/validation-errors]
 :<- [:form/server-errors]
 (fn [[validation server] _]
   (merge validation server)))

(rf/reg-sub
 :form/error
 :<- [:form/errors]
 (fn [errors [_ id]]
   (get errors id)))

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
   {:db {:messages/loading? true}
    :dispatch [:messages/load]}))

(rf/reg-event-fx
 :messages/load
 (fn [{:keys [db]} _]
  (GET "/api/messages"
       {:headers {"Accept" "application/transit+json"}
        :handler #(rf/dispatch [:messages/set (:messages %)])})
  {:db (assoc db :messages/loading? true)}))

(rf/reg-sub
 ;"Subscription for message loading"
 :messages/loading?
 (fn [db _]
   (:messages/loading? db)))

(defn home []
  (let [messages (rf/subscribe [:messages/list])]
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

(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (.log js/console "Mounting components")
  (dom/render [#'home] (.getElementById js/document "content"))
  (.log js/console "Components mounted!"))

(defn init! []
  (.log js/console "Initialising app")
  (rf/dispatch [:app/initialize])
  (mount-components))
