(ns homeworlds.core
  (:require [schema.core :as s]
            [datascript.core :as d]))



(s/defrecord Piece
    [id :- long
     size :- long
     color :- (s/enum :red :yellow :green :blue)])

(s/defrecord Ship
    [id :- long
     star :- long
     player :- long
     piece :- long])

(s/defrecord Star
    [id :- long
     gen-id :- long
     piece :- long])

(s/defrecord Homeworld
    [id :- long
     star :- long
     star-a :- Star
     star-b :- Star])


(def schema {:ship/star {:db/cardinality :db.cardinality/one}})
(def conn (-> (d/create-conn schema)
              ; create initial pieces
              (d/db-with
               (->>
                (for [size (range 1 4)
                      color [:red :yellow :green :blue]
                      qty (range 0 4)]
                  {:piece/size size :piece/color color})
                (map-indexed #(assoc %2 :db/id %1))))))


(defn- get-ships
  ([star]
   (d/q [:find ?ship
         :where
         [?ship :ship/star star]
         [?ship :ship/player player]] conn))
  ([star color]
   (d/q [:find ?ship
         :where
         [?ship :ship/star star]
         [?ship :ship/piece ?piece]
         [?piece :piece/color color]] conn)))

(defn- get-ships-player
  ([player star]
   (d/q [:find ?ship
         :where
         [?ship :ship/star star]
         [?ship :ship/player player]] conn))
  ([player star size]
   (d/q [:find ?ship
         :where [[?ship :ship/star star]
                 [?ship :ship/player player]
                 [?ship :ship/piece ?piece]
                 [?piece :piece/size ?size]]] conn)))

(defn- get-pieces
  ([star]
   (d/q [:find ?piece
         :where
         (or (and [?ship :ship/star star]
                  [?ship :ship/piece ?piece])
             [star :star/piece ?piece])]
        conn))
  ([star color]
   (d/q [:find ?piece
         :where
         (or (and [?ship :ship/star star]
                  [?ship :ship/piece ?piece]
                  [?piece :piece/color color])
             (and [star :star/piece ?piece]
                  [?piece :piece/color color]))]
        conn)))



(defn- get-unused-pieces
  ([]
   (d/q [:find ?piece
         :where
         (not [:ship/piece ?piece])
         (not [:star/piece ?piece])]
        conn))
  ([color]
   (d/q [:find ?piece
         :where
         [?piece :piece/color color]
         (not [:ship/piece ?piece])
         (not [:star/piece ?piece])]
        conn))
  ([color size]
   (d/q [:find ?piece
         :where
         [?piece :piece/color color]
         [?piece :piece/size size]
         (not [:ship/piece ?piece])
         (not [:star/piece ?piece])]
        conn)))

(defn move
  ([p star ship-size new-star-color new-star-size]
   (when-let [placeable-piece-of-size-and-color
              (first (get-unused-pieces new-star-color new-star-size))]
     (d/transact! conn [{:db/id (get-ships-player p star ship-size)
                         :ship/star -2}
                        {:db/id -2
                         :star/piece placeable-piece-of-size-and-color}])))
  ([p star ship-size target-star]
   (d/transact! conn [{:db/id (get-ships-player p star ship-size)
                       :ship/star target-star}])))

(defn construct [p star ship-size target-color]
  ; needs existing ship of same color in star
  ; get smallest ship of target-color from bank
  (when (some #(= (-> % :ship/piece :piece/color) target-color) (get-ships star))
      (when-let [smallest-placable-piece-of-color
                 (first (sort-by :piece/size (get-unused-pieces target-color)))]
        (d/transact! conn [{:db/id -1
                            :ship/star star
                            :ship/piece smallest-placable-piece-of-color
                            :ship/player p}]))))

(defn sacrifice [p star ship-size]
  (d/transact! conn [[:db.fn/retractEntity (:db/id (first (get-ships-player p star ship-size)))]]))

(defn trade [p star ship-size new-color]
  (when-let [piece-of-color-and-size (first (get-unused-pieces target-color ship-size))]
    (let [ship-id (s p star ship-size)]
      (d/transact! conn [{:db/id ship-id
                          :ship/piece piece-of-color-and-size}]))))

(defn attack [p star ship-size target-player target-ship-size]
  (let [ship-id (get-ships-player p star ship-size)
        largest-controlled-ship (first (sort-by #(-> % :ship/piece :piece/size) (get-player-ships-in-star p star)))]
    (when (<= (:piece/size (:ship/piece largest-controlled-ship)) target-ship-size)
      (d/transact! conn [{:db/id (get-ships-player target-player star target-ship-size)
                          :ship/player p}]))))

(defn catastrophe [star target-color]
  ; everything in star dies?
  ; star death v. ship death
  ; validate number of color at star
  (when (<= 4 (count (get-pieces star target-color)))
    (if (= target-color (-> (get-star star) :star/piece :piece/color))
      ;star death (everything there dies)
      (d/transact! conn (map (fn [id] [:db.fn/retractEntity id])
                             (conj (get-ships star) star)))
      ;ship death (only ships of color die)
      (d/transact! conn (map (fn [ship-id] [:db.fn/retractEntity ship-id]) (get-ships star color))))))

(defn event [e]
  (match
   [e]
   [[:construct]]
   [[:sacrifice]]
   [[:move]]
   [[:trade]]
   [[:attack]]
   [[:catastophe]]))



[
 ["setup"
  ["p1" "r1" "g2" "b"]
  ["p2 ""r1" "g2" "y"]]
 ["p1"
  ["construct" 0 "b"]]
 ["p2"
  ["construct" 1 "y"]]
 ["p1"
  ["move" 0 "y3" "r3"]
  ["catastrophe" 99 "y"]]
 ["p2"
  ["sacrifice" 0 "y3"]
  ["move" 99 "y3" "r8"]
  ["move" 99 "y3" "r8"]
  ["move" 99 "y3" "r8"]]]

