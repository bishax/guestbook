(ns guestbook.messages
  (:require
   [clojure.string :as string]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [guestbook.validation :refer [validate-message]]))

(rf/reg-event-fx
 :messages/load-by-author
 (fn [{:keys [db]} [_ author]]
   {:db (assoc db :messages/loading? true
                  :messages/list nil)
    :ajax/get {:url (str "/api/messages/by/" author)
               :success-path [:messages]
               :success-event [:messages/set]}}))

(rf/reg-event-fx
 :messages/load
 (fn [{:keys [db]} _]
   {:db       (assoc db
                     :messages/loading? true
                     :messages/list nil)
    :ajax/get {:url           "/api/messages"
               ; Event to dispatch on success
               :success-event [:messages/set]
               ; Path to data in response to pass to event
               :success-path  [:messages]}}))

(rf/reg-event-db
 ;"DB event for getting messages"
 :messages/set
 (fn [db [_ messages]]
   (-> db
       (assoc :messages/loading? false
              :messages/list messages))))

(rf/reg-sub
 ;"Subscription for message loading"
 :messages/loading?
 (fn [db _]
   (:messages/loading? db)))

(rf/reg-sub
 ;"Subscription for getting messages"
 :messages/list
 (fn [db _]
   (:messages/list db [])))

(defn reload-messages-button []
  (let [loading? (rf/subscribe [:messages/loading?])]
    [:button.button.is-info.is-fullwidth
     {:on-click #(rf/dispatch [:messages/load])
      :disabled @loading?}
     (if @loading?
       "Loading Messages"
       "Refresh Messages")]))

(defn message-list-placeholder []
  [:ul.messages
   [:li
    [:p "Loading Messages..."]
    [:div {:style {:width "10em"}}
     [:progress.progress.is-dark {:max 100} "30%"]]]])

(defn message-list [messages]
  [:ul.messages
   (for [{:keys [timestamp message name author]} @messages]
     ^{:key timestamp}
     [:li
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p " - " name
       " <"
       (if author
         [:a {:href (str "/user/" author)} (str "@" author)]
         [:span.is-italic "account not found"])
       ">"]])])

(rf/reg-event-db
 ; "DB event for adding new message"
 :message/add
 (fn [db [_ message]]
   (update db :messages/list conj message)))

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

(rf/reg-event-fx
 :message/send!-called-back
 (fn [_ [_ {:keys [success errors]}]]
   (if success
     (rf/dispatch [:form/clear-fields])
     (rf/dispatch [:form/set-server-errors errors]))))

(rf/reg-event-fx
 :message/send!
 (fn [{:keys [db]} [_ fields]]
   {:db (dissoc db :form/server-errors)
    :ws/send! {:message [:message/create! fields]
               :timeout 10000
               :callback-event [:message/send!-called-back]}}))

(defn errors-component [id & [message]]
  (when-let [error @(rf/subscribe [:form/error id])]
    [:div.notification.is-danger (if message
                                   message
                                   (string/join error))]))

(defn text-input [{type  :type
                   val :value
                   attrs :attrs
                   :keys [on-save]}]
  (let [draft (r/atom nil)
        value (r/track #(or @draft @val ""))]
    (fn []
      [type ;:input.input
       (merge attrs
              {:type :text
               ; clicking on text-area makes draft atom
               ; either empty or app-db value [:form/field :name]
               :on-focus #(reset! draft (or @val ""))
               ; clicking off text-area saves @draft
               ; (e.g. by saving to app-db) and empties @draft
               :on-blur (fn []
                          (on-save (or @draft ""))
                          (reset! draft nil))
               ; changes to value tracked in draft
               :on-change #(reset! draft (.. % -target -value))
               :value @value})])))

(defn message-form []
  [:div
   [errors-component :server-error]
   [errors-component :unauthorized "Please log in before posting."]
   [:div.field
    [:label.label {:for :name} "Name"]
    [errors-component :name]
    [text-input {:type :input.input
                 :attrs {:name :name}
                 :value (rf/subscribe [:form/field :name])
                 :on-save #(rf/dispatch [:form/set-field :name %])}]]
   [:div.field
    [:label.label {:for :message} "Message"]
    [errors-component :message]
    [text-input {:type :textarea.textarea
                 :attrs {:name :message}
                 :value (rf/subscribe [:form/field :message])
                 :on-save #(rf/dispatch [:form/set-field :message %])}]]
   [:input.button.is-primary
    {:type :submit
     :disabled @(rf/subscribe [:form/validation-errors?])
     :value "comment"
     :on-click #(rf/dispatch [:message/send! @(rf/subscribe [:form/fields])])}]])
