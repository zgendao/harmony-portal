(ns app.storage
  (:require
   [reagent.core :as reagent :refer [atom]]
   [app.api :as api :refer [init]]
   [alandipert.storage-atom :refer [local-storage]]))

; (def state (atom {:navbar-open false
;                   :modal nil
;                   :settings false}))
(def local (local-storage (atom {}) :local))
(def app-state (init {:apikey "test2"}))
