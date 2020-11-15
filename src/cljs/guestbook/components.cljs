(ns guestbook.components
  (:require
   [reagent.core :as r]))

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
