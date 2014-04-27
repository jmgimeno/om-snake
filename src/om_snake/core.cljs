(ns om-snake.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [goog.events :as events]
            [cljs.core.async :as async :refer [>! <! put! chan timeout]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(enable-console-print!)

(def width     20)
(def height    15)
(def size      10)
(def plank    150)

(def keycode->direction {37 :left 38 :up 39 :right 40 :down})

(defn place-not-in [forbidden]
  (first (drop-while #(some #{%} forbidden)
                     (repeatedly #(mapv rand-int [width height])))))

(defn n-places-not-in [n forbiden]
  (first (drop-while #(not= (count %) n)
                     (iterate #(conj % (place-not-in forbiden)) #{}))))

(defn init-game []
  (let [snake (place-not-in [])
        food  (place-not-in [snake])]
    {:food      food
     :snake     [snake]
     :dead      false
     :direction (rand-nth (vals keycode->direction))}))

; Game dynamics

(defn one-step [[x y] direction]
  (condp = direction
    :left  [(dec x)    y   ]
    :up    [   x    (dec y)]
    :right [(inc x)    y   ]
    :down  [   x    (inc y)]))

(defn next-head [[head] direction]
  (mapv mod (one-step head direction) [width height]))

(defn grow-snake [{:keys [snake food] :as state} next-head]
  (let [new-snake (into [next-head] snake)
        new-food  (place-not-in new-snake)]
    (assoc state :snake new-snake :food new-food)))

(defn kill-snake [state]
  (assoc state :dead true))

(defn move-snake [{:keys [snake] :as state} next-head]
  (assoc state :snake (into [next-head] (butlast snake))))

(defn game-step [{:keys [snake food direction] :as state}]
  (let [new-head (next-head snake direction)]
    (cond
      (= food new-head)        (grow-snake state new-head)
      (some #{new-head} snake) (kill-snake state)
      :else                    (move-snake state new-head))))

; Interaction

(def app-state (atom (init-game)))

(defn listen [el type]
  (let [out (chan)]
    (events/listen el type #(put! out %))
    out))

(defn render-cells [cells type]
  (for [[x y] cells]
    [:rect {:x (* x size) :y (* y size) :height size :width size :class type}]))

(defn world [{:keys [food snake dead]} _]
  (reify
    om/IRender
    (render [_]
      (html
        [:svg {:width (* width size) :height (* height size)}
         (render-cells [food] "food")
         (render-cells snake (if dead "dead" "alive"))]))))

(defn root [state _]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [keyboard-channel (listen js/window "keydown")]
        (go (loop [step-channel (timeout plank)]
              (alt!
                keyboard-channel ([e c]
                                    (when-let [direction (keycode->direction (.-keyCode e))]
                                      (om/update! state [:direction] direction))
                                    (recur step-channel))
                step-channel ([e c]
                              (when-not (:dead @state)
                                (om/transact! state game-step)
                                (recur (timeout plank)))))))))

    om/IRender
    (render [_]
      (html [:div
             [:h1 "Welcome to Snake !!"]
             [:div
              (str "Snake length: "
                   (count (:snake state))
                   (when (:dead state) " ENDGAME !!"))]
             (om/build world state)]))))

(om/root
  root
  app-state
  {:target (. js/document (getElementById "app"))})
