(defproject homeworlds "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170" :scope "provided"]
                 [org.clojure/core.memoize "0.5.8"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [datascript "0.13.3"]

                 [prismatic/schema "1.0.3"]
                 ;; ---------- Dev tools ----------
                 [lein-cljsbuild      "1.1.1"] ]

  :plugins [[lein-environ    "1.0.1"]
            [cider/cider-nrepl "0.10.0"]
            [lein-cljsbuild  "1.1.1"]
            [lein-doo "0.1.5" :exclusions [joda-time org.clojure/tools.reader com.fasterxml.jackson.core/jackson-core]]
            [com.aphyr/prism "0.1.3" :exclusions [fs]]
            [lein-ancient    "0.6.7"]]

  :hooks [leiningen.cljsbuild]

  :clean-targets ^{:protect false}
  [:target-path :compile-path "resources/public/out" "resources/public/min" "resources/private/out"]

  :jvm-opts ["-Xmx8g"]
  :cljsbuild
  {:builds
   [{:id "main"
     :source-paths ["src"]
     :compiler     {:output-to "resources/public/out/main.js"
                    :output-dir "resources/public/out"
                    :optimizations :whitespace ;; ok
                    ;; :optimizations :simple ;; ok - minor warnings, from encore, of course.
                    ;; :optimizations :advanced ;; couple of extern issues.
                    :pretty-print true}}]}

  :main homeworlds.core)
