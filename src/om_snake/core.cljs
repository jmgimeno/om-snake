(ns om-snake.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [goog.events :as events]
            [cljs.core.async :as async :refer [>! <! put! chan timeout]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(enable-console-print!)

(def height 40)
(def width 40)
(def size 10)
(def plank 100)

(def keycodes {37 :left 38 :up 39 :right 40 :down})

(def app-state (atom {:food nil
                      :snake []
                      :direction :up}))

(defn random-place-not-in [forbidden]
  (first (drop-while (fn [place] (some #{place} forbidden))
                     (repeatedly (fn [] [(rand-int width)
                                         (rand-int height)])))))

(defn do-place-food [{:keys [snake] :as state}]
  (om/update! state [:food] (random-place-not-in snake)))

(defn do-init-game [state]
  (let [snake (random-place-not-in [])
        food  (random-place-not-in [snake])]
    (om/update! state [:food] food)
    (om/update! state [:snake] [snake])))


(defn listen [el type]
  (let [out (chan)]
    (events/listen el type #(put! out %))
    out))

(defn render-cells [cells type]
  (for [[x y] cells]
    [:rect {:x (* x size) :y (* y size) :height size :width size :class type}]))

(defn world [{:keys [food snake]} _]
  (reify
    om/IRender
    (render [this]
      (html
        [:svg {:width (* width size) :height (* width size)}
         (render-cells [food] "food")
         (render-cells snake "snake")]))))

(defn one-step [[x y] direction]
  (condp = direction
    :left  [(dec x) y]
    :up    [x (dec y)]
    :right [(inc x) y]
    :down  [x (inc y)]))

(defn next-head [head direction]
  (let [[x y] (one-step head direction)]
    [(mod x width) (mod y height)]))

(defn next-snake [[head & tail] direction]
  (into [(next-head head direction)] (butlast tail)))

(defn do-move [{:keys [direction] :as state}]
  (update-in state [:snake] next-snake direction))

(defn root [state _]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [keyboard-channel (listen js/window "keydown")]
        (do-init-game state)
        (go (loop [step-channel (timeout plank)]
              (alt!
                keyboard-channel ([e c]
                                    (when-let [direction (keycodes (.-keyCode e))]
                                      (om/update! state [:direction] direction))
                                    (recur (timeout plank)))
                step-channel ([e c]
                                    (om/transact! state do-move)
                                    (recur (timeout plank))))))))

    om/IRender
    (render [_]
      (html [:div
             [:h1 "Welcome to Snake !!"]
             [:div
              (str "Snake length: " (count (:snake state))
                   " Direction: " (:direction state))]
             (om/build world state)]))))

(om/root
  root
  app-state
  {:target (. js/document (getElementById "app"))})
