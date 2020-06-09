(ns app.views
  (:require [reagent.core :as reagent :refer [atom]]
            [app.storage :refer [app-state local]]
            [app.request :refer [data get-accounts]]
            [goog.string :as gstring :refer [format]]
            [goog.string.format]
            [clojure.string :as str :refer [includes? lower-case replace]])
  (:import [goog.async Debouncer]))

(def state (atom {:navbar-open false
                  :logged-in false
                  :loginPanel false
                  :validatorPanel false
                  :settingsPanel false}))
(def review (atom ""))
(def recommend (atom true))

(defn commas [num]
  (replace (str num) #"\B(?=(\d{3})+(?!\d))" ","))
(defn vformat [value]
  (when value (commas (format "%.0f" value))))
(defn pformat [percent]
  (when percent (format "%.2f%" (* 100 percent))))

(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

(defn open-modal [modal id]
  (do
    (swap! state assoc modal id)
    (set! (.. js/document -body -style -overflow) "hidden")))

(defn close-modal [modal]
  (do
    (swap! state assoc modal false)
    (set! (.. js/document -body -style -overflow) "auto")
    (reset! review "")
    (reset! recommend true)))

(defn login []
  ;(swap! state assoc :logged-in true)
  (close-modal :loginPanel))

(defn already-rated [id]
  (if (map? (get-in @local [:reviews (keyword id)])) true false))

(defn rate [address recommend review]
  (swap! local assoc-in [:reviews (keyword address)] {:recommend recommend :review review})
  (swap! app-state assoc-in [:reviews (keyword address) (.getTime (js/Date.))] {:recommend recommend :review review}))

(defn loading []
  [:p "loading.."])

(defn navbar []
  [:nav
   ;[:p (str @app-state)]
   ;(reset! app-state {:reviews {}})
   [:div {:bp "container flex"}
    [:div.navbar__brand
     [:a {:href "https://harmony.one/" :target "_blank" :bp "flex"}
      [:img {:src "./images/logo1.png" :width "25px" :height "25px"}]
      [:p "Harmony Validator Community"]]]
    [:div.collapse {:bp "flex" :class [(when (@state :navbar-open) "u-show")]}
     [:div {:class [(when (get-in @local [:own-validator :address]) "u-show")]}
      [:a {:href "#" :on-click #(open-modal :settingsPanel true)} "Settings"]
      [:a {:href (str (:account @local))} "Address"]]
     [:div {:class [(when-not (get-in @local [:own-validator :address])) "u-show"]}
      [:a {:href "#" :on-click #(open-modal :loginPanel true)} "Login"]]]
    [:button.navbar__togglr {:bp "hide@md"
                             :on-click #(swap! state assoc :navbar-open (not (@state :navbar-open)))}
     [:span.navbar__togglr__bars]
     [:span.navbar__togglr__bars]
     [:span.navbar__togglr__bars]]]])

(defn loginPanel []
  [:div.modalWrapper {:class (when (@state :loginPanel) "u-show")
                      :on-click #(close-modal :loginPanel)}
   [:div#loginPanel.card.modal {:on-click (fn [e] (.stopPropagation e))}
    [:h2.title "Login with Harmony Browser Extension"]
    [:p "Here's the list of accounts we've received from the Harmony Browser Extension. Click the one you want to use!"]
    [:div.card {:on-click #(login)}
     [:h3 "Brunya"]
     [:p "one1d6vmmq6...maxy330d"]]]])

(defn settingsPanel []
  (let [description (atom (get-in @local [:own-validator :description]))
        whoareyou (atom (get-in @local [:own-validator :whoareyou]))
        projects (atom (get-in @local [:own-validator :projects]))]
    (fn []
      [:div.modalWrapper {:class (when (@state :settingsPanel) "u-show")
                          :on-click #(close-modal :settingsPanel)}
       [:div#settings.card.modal {:bp "container" :on-click (fn [e] (.stopPropagation e))}
        [:h2.title "Settings"]
        [:button.modal__closeBtn {:on-click #(close-modal :settingsPanel)} "X"]
        [:label.label--large "Description"
         [:textarea {:rows "3"
                     :placeholder "Write an answer"
                     :value @description
                     :on-change #(reset! description (-> % .-target .-value))}]]
        [:label.label--large "Who are you?"
         [:textarea {:rows "3"
                     :placeholder "Write an answer"
                     :value @whoareyou
                     :on-change #(reset! whoareyou (-> % .-target .-value))}]]
        [:label.label--large "Are you staking in another projects?"
         [:textarea {:rows "3"
                     :placeholder "Write an answer"
                     :value @projects
                     :on-change #(reset! projects (-> % .-target .-value))}]]
        [:div.settings__bottom
         [:input {:type "submit"
                  :value "Save Changes"
                  :on-click #(do (swap! local assoc-in [:own-validator :description] @description)
                                 (swap! local assoc-in [:own-validator :whoareyou] @whoareyou)
                                 (swap! local assoc-in [:own-validator :projects] @projects))}]]]])))

(defn validatorPanel []
  (let [address (:validatorPanel @state)
        validator (get @data address)
        reviews (get-in @app-state [:reviews address])
        rec (reduce-kv #(if (%3 :recommend) (inc %1) %1) 0 reviews)
        warn (reduce-kv #(if (%3 :recommend) %1 (inc %1)) 0 reviews)
        image (str "https://raw.githubusercontent.com/harmony-one/validator-logos/master/validators/" (name (or address "")) ".jpg")
        generated (str "https://avatars.dicebear.com/api/jdenticon/" (name (or address "")) ".svg")]
    [:div.modalWrapper {:class (when (@state :validatorPanel) "u-show")
                        :on-click #(close-modal :validatorPanel)}
     [:div#validator.card.modal {:bp "padding--none container" :on-click (fn [e] (.stopPropagation e))}
      [:div {:bp "grid gap-none"}
       [:div.validator__header__section {:bp "12 6@lg flex" :style {:border-right "1px solid var(--border-color)"}}
        [:div.validator__header__profileWrapper.u-fit {:bp "flex padding--lg text-center"}
         [:div {:style {:margin "auto"}}
          (when address [:img {:src image :onError #(set! (-> % .-target .-src) generated) :width "25px" :height "25px"}])
          [:p.u-cutText {:title (:name validator)} (:name validator)]]]
        [:div {:bp "fill"}
         [:p "Total staked:"]
         [:p "Delegated:"]
         [:p "Self stake:"]
         [:p "Max delegation:"]]
        [:div {:bp "fill"}
         [:p [:b (vformat (:total-stake validator))]]
         [:p [:b (vformat (:total-delegation validator))]]
         [:p [:b (vformat (:self-stake validator))]]
         [:p [:b (vformat (:max-delegation validator))]]]]
       [:div.validator__header__section.u-noWrap {:bp "12 6@lg flex"}
        [:div.u-fit
         [:p "Address:"]
         [:p "Website:"]
         [:p "Fee:"]
         [:p "Uptime:"]]
        [:div.u-cutText {:style {:max-width "unset"}}
         [:p.u-cutText {:title address} [:b {:style {:color "var(--primary-color)"}} address]]
         [:p {:title (:website validator)}
          [:a {:href (:website validator) :target "_BLANK"} [:b (:website validator)]]]
         [:p [:b (pformat (:fee validator))]]
         [:p [:b (pformat (:uptime validator))]]]]]
      [:div {:bp "grid gap-none"}
       [:div {:bp "12 8@md"}
        [:p.text--larger (:description validator)]
        [:h3 "Who are you?"]
        [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Enim volutpat,
                 vivamus ac et orci eros sagittis arcu amet. Eu viverra in pellentesque
                 pretium sagittis lacus."]
        [:h3 "Are you staking in another projects?"]
        [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sit urna iaculis
                 fames elementum tempor aliquam, pretium. Consequat dolor, ac pulvinar praesent
                 lectus ipsum consectetur tellus."]]
       [:div {:bp "12 4@md"}]]
      [:div.validator__reviews
       [:div.validator__reviews__header
        [:img {:src "/images/recommend_icon.svg"}]
        [:span (if rec rec 0)]
        [:img {:src "/images/warning_icon.svg"}]
        [:span (if warn warn 0)]
        [:h3 (str (+ rec warn) " REVIEWS")]]
       (when-not (already-rated address)
         [:div.validator__reviews__controls
          [:div
           [:button {:class (when @recommend "input--active") :type "button" :on-click #(reset! recommend true)}
            [:img {:src "/images/recommend_icon.svg"}]
            "Recommend"]
           [:button {:class (when-not @recommend "input--active") :type "button" :on-click #(reset! recommend false)}
            [:img {:src "/images/warning_icon.svg"}]
            "Warning"]]
          [:div {:bp "fill"}
           [:input {:type "text"
                    :placeholder "Write a review"
                    :value @review
                    :on-change #(reset! review (-> % .-target .-value))}]
           [:input {:class "input--active"
                    :type "submit"
                    :value "Send"
                    :on-click #(rate address @recommend @review)}]]])
       [:div.validator__reviews__reviews
        (for [[k v] (get-in @app-state [:reviews address])]
          ^{:key k}
          [:div.card.review
           [:div.review__rating
            [:img {:src (str "/images/" (if (:recommend v) "recommend" "warning") "_icon.svg") :width "26px" :height "26px"}]]
           [:div.review__delegated
            [:div
             [:p "Delegated:"]
             [:strong "160,000 ONE"]]]
           [:div.review__content
            [:p (:review v)]]
           [:div.review__author
            [:p "oneq3xye3"]
            (let [d (js/Date. k)]
              [:p (str (.getFullYear d) "." (.slice (str "0" (inc (.getMonth d))) (- 2)) "." (.slice (str "0" (.getDate d)) (- 2)))])]])]]
      [:button.modal__closeBtn {:on-click #(close-modal :validatorPanel)} "X"]]]))

(def search (atom ""))
(def active (atom true))
(def column-sort (atom {:column "return" :inc true}))
(defn switch-sort [column]
  (swap! column-sort assoc :column column :inc (if (= (:column @column-sort) column) (not (:inc @column-sort)) true)))

(defn main []
  (let [search (atom "")
        active-filter (atom true)
        _ (when-not (get-in @local [:own-validator :description]) (swap! local assoc-in [:own-validator :description] (str (:description ((keyword (get-in @local [:own-validator :address])) @data)))))]
    (fn []
      [:main
       [loginPanel]
       [settingsPanel]
       [validatorPanel]
       [:div {:bp "container"}
        [:div#hotValidators
         [:h2.title "Hot Validators"]
         [:div.cardWrapper
          (for [[address validator] (take-last 5 (filter (fn [[k v]] (if (:active v) k)) @data))]
            (let [image (str "https://raw.githubusercontent.com/harmony-one/validator-logos/master/validators/" (name address) ".jpg")
                  generated (str "https://avatars.dicebear.com/api/jdenticon/" (name address) ".svg")]
              ^{:key address}
              [:div.card {:on-click #(open-modal :validatorPanel address)}
               [:img {:src image :onError #(set! (-> % .-target .-src) generated) :width "25px" :height "25px"}]
               [:h4.u-cutText {:title (:name validator)} (:name validator)]
               [:p (str "Fee: " (pformat (:fee validator))) [:br] (str "Return: " (pformat (:return validator)))]]))]]
        [:div#topValidators.card {:bp "padding--none"}
         [:div.topValidators__header {:bp "flex"}
          [:h2.title "Top validators"
           [:span (count @data)]]
          [:div {:bp "flex"}
           [:label.switch "Active"
            [:input {:type "checkbox" :checked @active :on-click #(reset! active (not @active))}]
            [:span.slider]]
           [:input.u-rounded {:type "search"
                              :placeholder "Search"
                              :value @search
                              :on-change #(debounce (reset! search (-> % .-target .-value)) 5000)}]]]
         [:div {:style {:overflow-x "auto"}}
          [:table
           [:thead
            [:tr
             [:th {:colSpan 2 :on-click #(switch-sort "rec")} "Rate"]
             [:th.table__nameRow {:on-click #(switch-sort "name")} "Name"]
             [:th {:on-click #(switch-sort "return")} "Expected return"]
             [:th {:on-click #(switch-sort "total-stake")} "Stake"]
             [:th {:on-click #(switch-sort "fee")} "Fees"]
             [:th {:on-click #(switch-sort "uptime")} "Uptime"]]]
           [:tbody
            (let [merged-data (into {} (map (fn [[k v]]
                                              {k {:active (get-in @data [k :active])
                                                  :name (get-in @data [k :name])
                                                  :return (get-in @data [k :return])
                                                  :total-stake (get-in @data [k :total-stake])
                                                  :fee (get-in @data [k :fee])
                                                  :uptime (get-in @data [k :uptime])
                                                  :rec (reduce-kv #(if (%3 :recommend) (inc %1) %1) 0 v)
                                                  :warn (reduce-kv #(if (%3 :recommend) %1 (inc %1)) 0 v)}})
                                            (:reviews @app-state)))
                  _ (println merged-data)]
              (doall
               (for [[address validator] (sort-by (comp (keyword (:column @column-sort)) second) #(if (:inc @column-sort) (compare %2 %1) (compare %1 %2)) merged-data)]
                 (when (and
                        (includes? (lower-case (str (:name validator))) (lower-case @search))
                        (if @active (:active validator) true))
                   ^{:key address}
                   [:tr {:on-click #(open-modal :validatorPanel address)}
                    [:td [:img {:src "/images/recommend_icon.svg"}] (:rec validator)]
                    [:td [:img {:src "/images/warning_icon.svg"}] (:warn validator)]
                    [:td.u-cutText {:title (:name validator)} (:name validator)]
                    [:td (pformat (:return validator))]
                    [:td (vformat (:total-stake validator))]
                    [:td (pformat (:fee validator))]
                    [:td (pformat (:uptime validator))]]))))]]]]]])))

(defn portal []
  [:<>
   [navbar]
   (if (first @data)
     [main]
     [loading])])
