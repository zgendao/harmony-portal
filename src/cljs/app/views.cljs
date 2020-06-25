(ns app.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [app.storage :refer [app-state local state]]
            [app.request :refer [data]]
            [goog.string :as gstring :refer [format]]
            [goog.string.format]
            [clojure.string :as str :refer [includes? lower-case replace]]
            [async-interop.interop :refer [<p!]])
  (:import [goog.async Debouncer]))

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
  (go
    (if (.-harmony js/window)
      (when-not (:account @local)
        (let [account (js->clj (<p! (.getAccount (.-harmony js/window))))
              address (get account "address")
              addresses (into [] (map (fn [[k v]] (name k))) @data)]
          (do (swap! local assoc :account address)
              (when (some #(= address %) addresses)
                (swap! local assoc :user-is-validator true)))))
      (open-modal :loginPanel true))))

(defn already-rated [address]
  (let [delegators (into [] (map (fn [[k v]] (name (:account v))) (get-in @app-state [address :reviews])))]
    (some #(= (@local :account) %) delegators)))

(defn rate [address recommend review]
  (swap! local assoc-in [:reviews (keyword address)] {:recommend recommend :review review})
  (swap! app-state assoc-in [(keyword address) :reviews (.getTime (js/Date.))] {:account (:account @local) :recommend recommend :review review}))

(defn copy-to-clipboard [val]
  (let [temp (js/document.createElement "textarea")]
    (set! (.-value temp) val)
    (.appendChild js/document.body temp)
    (.select temp)
    (js/document.execCommand "copy")
    (.removeChild js/document.body temp)))

(defn loading []
  [:div.loadingScreen
   [:svg {:viewBox "0 0 100 100" :xmlns "http://www.w3.org/2000/svg"}
    [:circle {:cx "50" :cy "50" :r "45"}]]])

(defn twitter-timeline [url]
  (fn []
    (reagent/create-class
     {:display-name "twitter-timeline"
      :component-did-mount #(let [script (.createElement js/document "script")]
                              (do
                                (set! (.-src script) "https://platform.twitter.com/widgets.js")
                                (.appendChild
                                 (aget (.getElementsByClassName js/document "twitter-embed") 0)
                                 script)))
      :reagent-render (fn [] [:section.twitterContainer
                              [loading]
                              [:div.twitter-embed
                               [:a.twitter-timeline
                                {:href url
                                 :data-chrome "noheader nofooter noborders"
                                 :data-lang "en"
                                 :data-width "100%"
                                 :data-height "100%"}
                                ""]]])})))

(defn navbar []
  [:nav
   [:div {:bp "container flex"}
    [:div.navbar__brand
     [:a {:href "https://harmony.one/" :target "_blank" :bp "flex"}
      [:img {:src "./images/logo1.png" :width "25px" :height "25px"}]
      [:p "Harmony Validator Community"]]]
    [:div.collapse {:bp "flex" :class [(when (@state :navbar-open) "u-show")]}
     [:div {:class [(when (:account @local) "u-show")]}
      (when (@local :user-is-validator)
        [:p
         [:a {:on-click #(open-modal :settingsPanel true)} "Settings"]])
      [:p.tooltip
       [:a.u-cutText {:data-tooltip "Click to copy" :data-address (str (:account @local))
                      :on-click #(do (copy-to-clipboard (:account @local))
                                     (-> % .-target (.setAttribute "data-tooltip" "Copied")))
                      :on-mouse-out #(-> % .-target (.setAttribute "data-tooltip" "Click to copy"))}
        "Your address"]]]
     [:div {:class [(when-not (:account @local) "u-show")]}
      [:a {:on-click #(login)} "Login"]]]
    [:button.navbar__togglr {:bp "hide@md"
                             :on-click #(swap! state assoc :navbar-open (not (@state :navbar-open)))}
     [:span.navbar__togglr__bars]
     [:span.navbar__togglr__bars]
     [:span.navbar__togglr__bars]]]])

(defn loginPanel []
  [:div.modalWrapper {:class (when (@state :loginPanel) "u-show")
                      :on-click #(close-modal :loginPanel)}
   [:div#loginPanel.card.modal {:on-click (fn [e] (.stopPropagation e))}
    [:h2.title "Login with your Math Wallet"]
    [:a {:href "https://chrome.google.com/webstore/detail/math-wallet/afbcbjpbpfadlkmhmclhkeeodmamcflc" :target "_blank"} "Chrome Extension"]]])

(defn settingsPanel []
  (let [description (atom (get-in @app-state [(keyword (:account @local)) :description]))
        whoareyou (atom (get-in @app-state [(keyword (:account @local)) :whoareyou]))
        projects (atom (get-in @app-state [(keyword (:account @local)) :projects]))
        twitter (atom (get-in @app-state [(keyword (:account @local)) :twitter]))]
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
        [:label.label--large "What's your Twitter?"
         [:input {:type "text"
                  :placeholder "Paste your profile URL"
                  :value @twitter
                  :on-change #(reset! twitter (-> % .-target .-value))}]]
        [:div.settings__bottom
         [:button {:type "submit"
                   :on-click #(do (swap! app-state assoc-in [(keyword (:account @local)) :description] @description)
                                  (swap! app-state assoc-in [(keyword (:account @local)) :whoareyou] @whoareyou)
                                  (swap! app-state assoc-in [(keyword (:account @local)) :projects] @projects)
                                  (swap! app-state assoc-in [(keyword (:account @local)) :twitter] @twitter)
                                  (-> % .-target (.setAttribute "class" "button--done"))
                                  (close-modal :settingsPanel))
                   :on-mouse-out #(-> % .-target (.setAttribute "class" ""))}
          "Save Changes"]]]])))

(defn validatorPanel []
  (let [address (:validatorPanel @state)
        validator (get @data address)
        reviews (get-in @app-state [address :reviews])
        rec (reduce-kv #(if (%3 :recommend) (inc %1) %1) 0 reviews)
        warn (reduce-kv #(if (%3 :recommend) %1 (inc %1)) 0 reviews)
        image (str "https://raw.githubusercontent.com/harmony-one/validator-logos/master/validators/" (name (or address "")) ".jpg")
        generated (str "https://avatars.dicebear.com/api/jdenticon/" (name (or address "")) ".svg")
        sett-desc (get-in @app-state [address :description])
        sett-who (get-in @app-state [address :whoareyou])
        sett-proj (get-in @app-state [address :projects])
        sett-twitter (get-in @app-state [address :twitter])]
    [:div.modalWrapper {:class (when (@state :validatorPanel) "u-show")
                        :on-click #(close-modal :validatorPanel)}
     [:div#validator.card.modal {:bp "padding--none container" :on-click (fn [e] (.stopPropagation e))}
      [:div {:bp "grid gap-none"}
       [:div.validator__header__section {:bp "12 6@lg flex"}
        [:div.validator__header__profileWrapper {:bp "flex text-center"}
         [:div {:style {:margin "auto"}}
          (when address [:img {:src image :onError #(set! (-> % .-target .-src) generated) :width "25px" :height "25px"}])
          [:p.u-cutText {:title (:name validator)} (:name validator)]]]
        [:div.validator__header__labelWrapper
         [:p "Total staked:"]
         [:p "Delegated:"]
         [:p "Self stake:"]
         [:p "Max delegation:"]]
        [:div.validator__header__dataWrapper
         [:p [:b (vformat (:total-stake validator))]]
         [:p [:b (vformat (:total-delegation validator))]]
         [:p [:b (vformat (:self-stake validator))]]
         [:p [:b (vformat (:max-delegation validator))]]]]
       [:div.validator__header__section.u-noWrap {:bp "12 6@lg flex"}
        [:div.validator__header__labelWrapper
         [:p "Address:"]
         [:p "Website:"]
         [:p "Fee:"]
         [:p "Uptime:"]]
        [:div.validator__header__dataWrapper
         [:div.tooltip
          [:p.u-cutText
           [:a {:data-tooltip "Click to copy"
                :on-click #(do (copy-to-clipboard (name address))
                               (-> % .-target (.setAttribute "data-tooltip" "Copied")))
                :on-mouse-out #(-> % .-target (.setAttribute "data-tooltip" "Click to copy"))}
            [:b address]]]]
         [:p.u-cutText {:title (:website validator)}
          [:a {:href (:website validator) :target "_BLANK"} [:b (:website validator)]]]
         [:p [:b (pformat (:fee validator))]]
         [:p [:b (pformat (:uptime validator))]]]]]
      [:div {:bp "grid gap-none"}
       [:div {:bp "12 8@md"}
        [:p.text--larger (if (or (nil? sett-desc) (= "" sett-desc)) "-" sett-desc)]
        [:div
         [:h3 "Who are you?"]
         [:p (if (or (nil? sett-who) (= "" sett-who)) "-" sett-who)]]
        [:div
         [:h3 "Are you staking in another projects?"]
         [:p (if (or (nil? sett-proj) (= "" sett-proj)) "-" sett-proj)]]]
       (when-not (or (nil? sett-twitter) (= "" sett-twitter))
         [:div {:bp "12 4@md"}
          [twitter-timeline sett-twitter]])]
      [:div.validator__reviews
       [:div.validator__reviews__header
        [:img {:src "/images/recommend_icon.svg"}]
        [:span (if rec rec 0)]
        [:img {:src "/images/warning_icon.svg"}]
        [:span (if warn warn 0)]
        [:h3 (str (+ rec warn) " REVIEWS")]]
       (when (and (not (already-rated address)) (:account @local))
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
        (doall
         (for [[k v] (into (sorted-map-by >) (get-in @app-state [address :reviews]))]
           (let [delegated (first (filter #(when (= (:account v) (:delegator-address %)) %) (get-in @data [address :delegations])))
                 shift 0.000000000000000001]
             ^{:key k}
             [:div.card.review
              [:div.review__rating
               [:img {:src (str "/images/" (if (:recommend v) "recommend" "warning") "_icon.svg") :width "26px" :height "26px"}]]
              [:div.review__delegated
               [:div
                (if (:amount delegated)
                  [:div
                   [:p "Delegated:"]
                   [:strong (format "%.2f" (* shift (:amount delegated)))]]
                  [:strong "Not delegated yet"])]]
              [:div.review__content
               [:p (:review v)]]
              [:div.review__author
               [:p (:delegator-address delegated)]
               (let [d (js/Date. k)]
                 [:p (str (.getFullYear d) "." (.slice (str "0" (inc (.getMonth d))) (- 2)) "." (.slice (str "0" (.getDate d)) (- 2)))])]])))]]
      [:button.modal__closeBtn {:on-click #(close-modal :validatorPanel)} "X"]]]))

(def search (atom ""))
(def active (atom true))
(def column-sort (atom {:column "return" :inc false}))
(defn switch-sort [column]
  (swap! column-sort assoc :column column :inc (if (= (:column @column-sort) column) (not (:inc @column-sort)) false)))

(defn main []
  (let [search (atom "")
        active (atom true)]
    (fn []
      (let [merged-data (into {} (map (fn [[k v]]
                                        {k {:active (get-in @data [k :active])
                                            :name (get-in @data [k :name])
                                            :return (get-in @data [k :return])
                                            :total-stake (get-in @data [k :total-stake])
                                            :fee (get-in @data [k :fee])
                                            :uptime (get-in @data [k :uptime])
                                            :rec (reduce-kv #(if (%3 :recommend) (inc %1) %1) 0 (:reviews v))
                                            :warn (reduce-kv #(if (%3 :recommend) %1 (inc %1)) 0 (:reviews v))}})
                                      @app-state))
            sorted (sort-by (comp (keyword (:column @column-sort)) second) #(if (:inc @column-sort) (compare %1 %2) (compare %2 %1)) merged-data)
            filtered (filter (fn [[k v]] (and (includes? (lower-case (str (:name v))) (lower-case @search))
                                              (if @active (:active v) true))) sorted)]
        [:main
         [loginPanel]
         [settingsPanel]
         [validatorPanel]
         [:div {:bp "container"}
          [:div#hotValidators
           [:h2.title "Hot Validators"
            [:span.title__icon.tooltip
             [:a {:data-tooltip "The newest active validators"} "i"]]]
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
             [:span (count filtered)]]
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
               [:th {:colSpan 2 :class [(when (= (:column @column-sort) "rec")
                                          (if (:inc @column-sort) "th--sortedBy inc" "th--sortedBy desc"))]}
                [:a {:on-click #(switch-sort "rec")} "Rate"]]
               [:th {:class [(when (= (:column @column-sort) "name")
                               (if (:inc @column-sort) "th--sortedBy inc" "th--sortedBy desc"))]}
                [:a {:on-click #(switch-sort "name")} "Name"]]
               [:th {:class [(when (= (:column @column-sort) "return")
                               (if (:inc @column-sort) "th--sortedBy inc" "th--sortedBy desc"))]}
                [:a {:on-click #(switch-sort "return")} "Expected return"]]
               [:th {:class [(when (= (:column @column-sort) "total-stake")
                               (if (:inc @column-sort) "th--sortedBy inc" "th--sortedBy desc"))]}
                [:a {:on-click #(switch-sort "total-stake")} "Stake"]]
               [:th {:class [(when (= (:column @column-sort) "fee")
                               (if (:inc @column-sort) "th--sortedBy inc" "th--sortedBy desc"))]}
                [:a {:on-click #(switch-sort "fee")} "Fees"]]
               [:th {:class [(when (= (:column @column-sort) "uptime")
                               (if (:inc @column-sort) "th--sortedBy inc" "th--sortedBy desc"))]}
                [:a {:on-click #(switch-sort "uptime")} "Uptime"]]]]
             [:tbody
              (doall
               (for [[address validator] filtered]
                 ^{:key address}
                 [:tr {:on-click #(open-modal :validatorPanel address)}
                  [:td [:img {:src "/images/recommend_icon.svg"}] (:rec validator)]
                  [:td [:img {:src "/images/warning_icon.svg"}] (:warn validator)]
                  [:td.u-cutText {:title (:name validator)} (:name validator)]
                  [:td (pformat (:return validator))]
                  [:td (vformat (:total-stake validator))]
                  [:td (pformat (:fee validator))]
                  [:td (pformat (:uptime validator))]]))]]]]]]))))

(defn portal []
  [:<>
   [navbar]
   (if (first @data)
     [main]
     [loading])])
