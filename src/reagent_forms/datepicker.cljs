(ns reagent-forms.datepicker
   (:require
   [clojure.string :as s]
   [reagent.core :refer [atom]]))

(def dates
  {:en-US {:days        ["Sunday" "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday"]
           :days-short  ["Su" "Mo" "Tu" "We" "Th" "Fr" "Sa"]
           :months      ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"]
           :months-short ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
           :first-day 0}
   :ru-RU {:days        ["воскресенье" "понедельник" "вторник" "среда" "четверг" "пятница" "суббота"]
           :days-short  ["Вс" "Пн" "Вт" "Ср" "Чт" "Пт" "Сб"]
           :months      ["Январь" "Февраль" "Март" "Апрель" "Май" "Июнь"  "Июль" "Август" "Сентябрь" "Октябрь" "Ноябрь" "Декабрь"]
           :months-short ["Янв" "Фев" "Мар" "Апр" "Май" "Июн"  "Июл" "Авг" "Сен" "Окт" "Ноя" "Дек"]
           :first-day 1}
   :fr-FR {:days        ["dimanche" "lundi" "mardi" "mercredi" "jeudi" "vendredi" "samedi"]
           :days-short  ["D" "L" "M" "M" "J" "V" "S"]
           :months      ["janvier" "février" "mars" "avril" "mai" "juin" "juillet" "août" "septembre" "octobre" "novembre" "décembre"]
           :months-short ["janv." "févr." "mars" "avril" "mai" "juin" "juil." "aût" "sept." "oct." "nov." "déc."]
           :first-day 1}
   :de-DE {:days        ["Sonntag" "Montag" "Dienstag" "Mittwoch" "Donnerstag" "Freitag" "Samstag"]
           :days-short  ["So" "Mo" "Di" "Mi" "Do" "Fr" "Sa"]
           :months      ["Januar" "Februar" "März" "April" "Mai" "Juni" "Juli" "August" "September" "Oktober" "November" "Dezember"]
           :months-short ["Jan" "Feb" "Mär" "Apr" "Mai" "Jun"  "Jul" "Aug" "Sep" "Okt" "Nov" "Dez"]
           :first-day 1}
   :es-ES {:days        ["domingo" "lunes" "martes" "miércoles" "jueves" "viernes" "sábado"]
           :days-short  ["D" "L" "M" "X" "J" "V" "S"]
           :months      ["enero" "febrero" "marzo" "abril" "mayo" "junio" "julio" "agosto" "septiembre" "octubre" "noviembre" "diciembre"]
           :months-short ["ene" "feb" "mar" "abr" "may" "jun"  "jul" "ago" "sep" "oct" "nov" "dic"]
           :first-day 1}
   :pt-PT {:days        ["Domingo" "Segunda-feira" "Terça-feira" "Quarta-feira" "Quinta-feira" "Sexta-feira" "Sábado"]
           :days-short  ["Dom" "Seg" "Ter" "Qua" "Qui" "Sex" "Sáb"]
           :months      ["Janeiro" "Fevereiro" "Março" "Abril" "Maio" "Junho" "Julho" "Agosto" "Setembro" "Outubro" "Novembro" "Dezembro"]
           :months-short ["Jan" "Fev" "Mar" "Abr" "Mai" "Jun" "Jul" "Ago" "Set" "Out" "Nov" "Dez"]
           :first-day 1}
   :fi-FI {:days        ["Sunnuntai" "Maanantai" "Tiistai" "Keskiviikko" "Torstai" "Perjantai" "Lauantai"]
           :days-short  ["Su" "Ma" "Ti" "Ke" "To" "Pe" "La"]
           :months      ["Tammikuu" "Helmikuu" "Maaliskuu" "Huhtikuu" "Toukokuu" "Kesäkuu" "Heinäkuu" "Elokuu" "Syyskuu" "Lokakuu" "Marraskuu" "Joulukuu"]
           :months-short ["Tammi" "Helmi" "Maalis" "Huhti" "Touko" "Kesä" "Heinä" "Elo" "Syys" "Loka" "Marras" "Joulu"]
           :first-day 1}
   :nl-NL {:days        ["zondag" "maandag" "dinsdag" "woensdag" "donderdag" "vrijdag" "zaterdag"]
           :days-short  ["zo" "ma" "di" "wo" "do" "vr" "za"]
           :months      ["januari" "februari" "maart" "april" "mei" "juni" "juli" "augustus" "september" "oktober" "november" "december"]
           :months-short ["jan" "feb" "maa" "apr" "mei" "jun" "jul" "aug" "sep" "okt" "nov" "dec"]
           :first-day 1}})

(defn separator-matcher [fmt]
  (if-let [separator (or (re-find #"[.\/\-\s].*?" fmt) " ")]
    [separator
     (condp = separator
       "." #"\."
       " " #"W+"
       (re-pattern separator))]))

(defn split-parts [fmt matcher]
  (->> (s/split fmt matcher) (map keyword) vec))

(defn parse-format [fmt]
  (let [fmt (or fmt "mm/dd/yyyy")
        [separator matcher] (separator-matcher fmt)
        parts (split-parts fmt matcher)]
    (when (empty? parts)
      (throw (js/Error. "Invalid date format.")))
    {:separator separator :matcher matcher :parts parts}))

(defn blank-date []
  (doto (js/Date.)
      (.setHours 0)
      (.setMinutes 0)
      (.setSeconds 0)
      (.setMilliseconds 0)))

(defn parse-date [date fmt]
  (let [parts (s/split date (:matcher fmt))
        date (blank-date)
        fmt-parts (count (:parts fmt))]
    (if (= (count (:parts fmt)) (count parts))
      (loop [year (.getFullYear date)
             month (.getMonth date)
             day (.getDate date)
             i 0]
        (if (not= i fmt-parts)
          (let [val (js/parseInt (parts i) 10)
                val (if (js/isNaN val) 1 val)
                part ((:parts fmt) i)]
            (cond
             (some #{part} [:d :dd]) (recur year month val (inc i))
             (some #{part} [:m :mm]) (recur year (dec val) day (inc i))
             (= part :yy)            (recur (+ 2000 val) month day (inc i))
             (= part :yyyy)          (recur val month day (inc i))))
          (js/Date. year month day 0 0 0)))
      date)))

(defn formatted-value [v]
  (str (if (< v 10) "0" "") v))

(defn format-date [{:keys [year month day]} {:keys [separator parts]}]
  (s/join separator
          (map
           #(cond
             (some #{%} [:d :dd]) (formatted-value day)
             (some #{%} [:m :mm]) (formatted-value month)
             (= % :yy)            (.substring (str year) 2)
             (= % :yyyy)          year)
           parts)))

(defn leap-year? [year]
  (or
   (and
     (= 0 (mod year 4))
     (not= 0 (mod year 100)))
   (= 0 (mod year 400))))

(defn days-in-month [year month]
  ([31 (if (leap-year? year) 29 28) 31 30 31 30 31 31 30 31 30 31] month))

(defn first-day-of-week [year month local-first-day]
  (let [day-num (.getDay (js/Date. year month 1))]
    (mod (- day-num local-first-day) 7)))

(defn gen-days [current-date get save! expanded? auto-close? local-first-day]
  (let [[year month day] @current-date
        num-days (days-in-month year month)
        last-month-days (if (pos? month) (days-in-month year (dec month)))
        first-day (first-day-of-week year month local-first-day)]
    (->>
      (for [i (range 42)]
        (cond
          (< i first-day)
          [:td.day.old
           (when last-month-days
             (- last-month-days (dec (- first-day i))))]
          (< i (+ first-day num-days))
          (let [day (inc (- i first-day))
                date {:year year :month (inc month) :day day}]
            [:td.day
             {:class (when-let [doc-date (get)]
                       (when (= doc-date date) "active"))
              :on-click #(do
                           (swap! current-date assoc-in [2] day)
                           (if (= (get) date)
                             (save! nil)
                             (save! date))
                           (when auto-close? (reset! expanded? false)))}
             day])
          :else
          [:td.day.new
           (when (< month 11)
             (inc (- i (+ first-day num-days))))]))
      (partition 7)
      (map (fn [week] (into [:tr] week))))))

(defn last-date [[year month day]]
  (if (pos? month)
    [year (dec month) day]
    [(dec year) 11 day]))

(defn next-date [[year month day]]
  (if (= month 11)
    [(inc year) 0 day]
    [year (inc month) day]))

(defn year-picker [date view-selector]
  (let [start-year (atom (- (first @date) 10))]
    (fn []
      [:table.table-condensed
       [:thead
        [:tr
         [:th.prev {:on-click #(swap! start-year - 10)} "‹"]
         [:th.switch
          {:col-span 2}
          (str @start-year " - " (+ @start-year 10))]
         [:th.next {:on-click #(swap! start-year + 10)} "›"]]]
       (into [:tbody]
             (for [row (->> (range @start-year (+ @start-year 12)) (partition 4))]
               (into [:tr]
                     (for [year row]
                       [:td.year
                        {:on-click #(do
                                      (swap! date assoc-in [0] year)
                                      (reset! view-selector :month))
                         :class (when (= year (first @date)) "active")}
                        year]))))])))

(defn month-picker [date view-selector {:keys [months-short]}]
  (let [year (atom (first @date))]
    (fn []
      [:table.table-condensed
       [:thead
        [:tr
         [:th.prev {:on-click #(swap! year dec)} "‹"]
         [:th.switch
          {:col-span 2 :on-click #(reset! view-selector :year)} @year]
         [:th.next {:on-click #(swap! year inc)} "›"]]]
       (into
         [:tbody]
         (for [row (->> months-short
                        (map-indexed vector)
                        (partition 4))]
           (into [:tr]
                 (for [[idx month-name] row]
                   [:td.month
                    {:class
                     (let [[cur-year cur-month] @date]
                       (when (and (= @year cur-year) (= idx cur-month)) "active"))
                     :on-click
                     #(do
                        (swap! date (fn [[_ _ day]] [@year idx day]))
                        (reset! view-selector :day))}
                    month-name]))))])))

(defn day-picker [date get save! view-selector expanded? auto-close? {:keys [months days-short first-day]}]
  (let [local-first-day first-day
        local-days-short (->> (cycle days-short)
                              (drop local-first-day) ; first day as offset
                              (take 7))]
    [:table.table-condensed
     [:thead
      [:tr
       [:th.prev {:on-click #(swap! date last-date)} "‹"]
       [:th.switch
        {:col-span 5
         :on-click #(reset! view-selector :month)}
        (str (nth months (second @date)) " " (first @date))]
       [:th.next {:on-click #(swap! date next-date)} "›"]]
      (into
        [:tr]
        (map-indexed (fn [i dow]
                     ^{:key i}  [:th.dow dow])
                     local-days-short))]
     (into [:tbody]
           (gen-days date get save! expanded? auto-close? local-first-day))]))

(defn datepicker [year month day dom-node mouse-on-list? expanded? auto-close? get save! inline lang]
  (let [date (atom [year month day])
        view-selector (atom :day)
        names (if (and (keyword? lang) (contains? dates lang))
                (lang dates)
                (if (every? #(contains? lang %) [:months :months-short :days :days-short :first-day])
                  lang
                  (:en-US dates)))]
    (fn []
      [:div {:class (str "datepicker" (when-not @expanded? " dropdown-menu") (if inline " dp-inline" " dp-dropdown"))
             :on-mouse-enter #(reset! mouse-on-list? true)
             :on-mouse-leave #(reset! mouse-on-list? false)
             :on-click       (fn [e]
                               (.preventDefault e)
                               (reset! mouse-on-list? true)
                               (.focus @dom-node))}
       (condp = @view-selector
         :day   [day-picker date get save! view-selector expanded? auto-close? names]
         :month [month-picker date view-selector names]
         :year  [year-picker date view-selector])])))
