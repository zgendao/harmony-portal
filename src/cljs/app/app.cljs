(ns app.app
  (:require
   [reagent.core :as r]
   [app.views :refer [portal]]
   [app.storage :refer [app-state]]
   [app.request :refer [get-validators fetch-validators]]
   [bide.core :as b]
   [app.peekaboo :as peekaboo]))

(enable-console-print!)

(defn transform-map [rmap]
  (into []
        (for [[r u] rmap]
          [u r]))) ""

(defn make-bide-router [rmap]
  (b/router (transform-map rmap)))

(def route-map {:app.home "/home"
                :app.about "/about"})

(def router (make-bide-router route-map))

; (defn nav-link [route text]
;   [:a {:href (route route-map)
;        :on-click #(do
;                     (-> % .preventDefault)
;                     (b/navigate! router route))} text])
;
; (defn page [name app-state]
;   [:div
;    [:nav
;     [:div [nav-link :app.home "Home"]]
;     [:div [nav-link :app.about "About"]]]
;    (case name
;      :app.home (home-page app-state)
;      :app.about [about-page])])

(defn on-navigate
  "A function which will be called on each route change."
  [name params query app-state]
  (r/render [portal app-state]
    (.getElementById js/document "app")))

(defn ^:export run [app-state]
  (b/start! router {:default :app.home
                    :html5? true
                    :on-navigate (fn [name params query] (on-navigate name params query app-state))})
  (get-validators)
  (fetch-validators)
  (peekaboo/gift "7d119aea-f0fb-4ecd-8771-d39978e06046"))

(run app-state)
