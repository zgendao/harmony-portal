(ns harmonyPortal.views
  (:require [reagent.core :as reagent :refer [atom]]
            [harmonyPortal.storage :refer [app-state local]]
            [harmonyPortal.request :refer [data request]]
            [goog.string :as gstring :refer [format]]
            [goog.string.format]
            [clojure.string :as str :refer [includes? lower-case replace]])
  (:import [goog.async Debouncer]))

(def state (atom {:navbar-open false
                  :modal nil
                  :settings false}))

(defn commas [num]
  (replace (str num) #"\B(?=(\d{3})+(?!\d))" ","))
(defn vformat [value]
  (when value (commas (format "%.0f" value))))
(defn pformat [percent]
  (when percent (format "%.2f%" (* 100 percent))))

(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

(defn open-modal [id]
  (do
    (swap! state assoc :modal id)
    (set! (.. js/document -body -style -overflow) "hidden")))

(defn close-modal []
  (do
    (swap! state assoc :modal nil)
    (set! (.. js/document -body -style -overflow) "auto")))

(defn loading []
  [:p "loading.."])

(defn navbar []
  [:nav
   [:div {:bp "container flex"}
    [:div.navbar__brand
     [:a {:href "https://harmony.one/" :target "_blank" :bp "flex"}
      [:img {:src "./images/logo1.png" :width "25px" :height "25px"}]
      [:p "Harmony Validator Community"]]]
    [:div.collapse {:bp "flex" :class [(when (@state :navbar-open) "u-show")]}
     [:a {:href "#" :on-click #(swap! state assoc :settings true)} "Settings"]
     [:a {:href "#"} "Address"]]
    [:button.navbar__togglr {:bp "hide@md"
                             :on-click #(swap! state assoc :navbar-open (not (@state :navbar-open)))}
     [:span.navbar__togglr__bars]
     [:span.navbar__togglr__bars]
     [:span.navbar__togglr__bars]]]])

(defn settings []
  [:div.modalWrapper {:style {:display (if (@state :settings) "block" "none")}}
   [:div#settings.card.modal {:bp "container"}
    [:h2.title "Settings"]
    [:button.modal__closeBtn {:on-click #(swap! state assoc :settings false)} "X"]
    [:label.label--large "Description"
     [:textarea {:rows "3" :placeholder "Write an answer"}]]
    [:label.label--large "Who are you?"
     [:textarea {:rows "3" :placeholder "Write an answer"}]]
    [:label.label--large "Are you staking in another projects?"
     [:textarea {:rows "3" :placeholder "Write an answer"}]]
    [:div.settings__bottom
     [:input {:type "submit" :value "Save Changes"}]]]])

(defn modal [id]
  (let [seq-map (into (hash-map) (map-indexed (fn [i value] [i value]) @data))
        validator (seq-map id)]
    [:div.modalWrapper {:style {:display (if (@state :modal) "block" "none")}}
     [:div.card.modal {:bp "padding--none container"}
      [:div {:bp "grid gap-none"}
       [:div {:bp "12 6@md flex" :style {:border-right "1px solid var(--border-color)"}}
        [:div.modal__header__profileWrapper.u-fit {:bp "flex padding--lg text-center"}
         [:div {:style {:margin "auto"}}
          [:img {:src "./images/logo1.png" :width "25px" :height "25px"}]
          [:p {:title (:name validator)} (:name validator)]]]
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
       [:div {:bp "12 6@md flex" :style {:justify-content "space-around"}}
        [:div.u-fit
         [:p "Address:"]
         [:p "Website:"]
         [:p "Fee:"]
         [:p "Uptime:"]]
        [:div
         [:p.u-cutText {:title (:address validator)} [:b {:style {:color "var(--primary-color)"}} (:address validator)]]
         [:a {:href (:website validator) :target "_BLANK"} [:b (:website validator)]]
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
      [:div.modal__reviews
       [:div.modal__reviews__header
        [:img {:src "/images/recommend_icon.svg"}]
        [:span "43"]
        [:img {:src "/images/warning_icon.svg"}]
        [:span "10"]
        [:h3 "2 REVIEWS"]]
       [:div.modal__reviews__controls
        [:div
         [:button {:class "input--active" :type "button"}
          [:img {:src "/images/recommend_icon.svg"}]
          "Recommend"]
         [:button {:type "button"}
          [:img {:src "/images/warning_icon.svg"}]
          "Warning"]]
        [:div {:bp "fill"}
         [:input {:type "text" :placeholder "Write a review"}]
         [:input {:class "input--active" :type "submit" :value "Send"}]]]
       [:div.modal__reviews__reviews
        [:div.card.review
         [:div.review__rating
          [:img {:src "/images/recommend_icon.svg" :width "26px" :height "26px"}]]
         [:div.review__delegated
          [:div
           [:p "Delegated:"]
           [:strong "160,000 ONE"]]]
         [:div.review__content
          [:p "I can only recommend them, great team!"]]
         [:div.review__author
          [:p "oneq3xye3"]
          [:p "2020.06.06"]]]]]
      [:button.modal__closeBtn {:on-click #(close-modal)} "X"]]]))

(defn main []
  (let [search (atom "")]
    [:main
     [settings]
     [modal (@state :modal)]
     [:div {:bp "container"}
      [:div#hotValidators
       [:h2.title "Hot Validators"]
       [:div.cardWrapper
        (for [validator (take-last 5 (into [] (filter #(%1 :active) @data)))]
          [:div.card {:on-click #(open-modal (.indexOf @data validator))}
           [:img {:src "./images/logo1.png" :width "25px" :height "25px"}]
           [:h4 (:name validator)]
           [:p (str "Fee:" (pformat (:fee validator))) [:br] (str "Return:" (pformat (:return validator)))]])]]
      [:div#topValidators.card {:bp "padding--none"}
       [:div.topValidators__header {:bp "flex"}
        [:h2.title "Top validators"
         [:span (count @data)]]
        [:input.u-rounded {:type "search"
                           :placeholder "Search"
                           :value @search
                           :on-change #(debounce (reset! search (-> % .-target .-value)) 5000)}]]
       [:div {:style {:overflow-x "auto"}}
        [:table
         [:thead
          [:tr
           [:th {:colSpan 2} "Rate"]
           [:th.table__nameRow "Name"]
           [:th "Expected return"]
           [:th "Stake"]
           [:th "Fees"]
           [:th "Uptime"]]]
         [:tbody
          (keep-indexed
           (fn [i validator]
             (when (includes? (lower-case (:name validator)) (lower-case @search))
               [:tr {:on-click #(open-modal i)}
                [:td [:img {:src "/images/recommend_icon.svg"}] "10"]
                [:td [:img {:src "/images/warning_icon.svg"}] "10"]
                [:td.table__nameRow {:title (:name validator)} (:name validator)]
                [:td (pformat (:return validator))]
                [:td (vformat (:total-stake validator))]
                [:td (pformat (:fee validator))]
                [:td (pformat (:uptime validator))]]))
           @data)]]]]]]))

(defn portal []
  [:<>
   [navbar]
   (if (first @data)
     [main]
     [loading])])
