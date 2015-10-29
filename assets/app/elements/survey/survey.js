angular.module("wust.elements").directive("survey", survey);

survey.$inject = [];

angular.module("wust.elements").service("SurveyService", SurveyService);

SurveyService.$inject = ["store"];

function SurveyService(store) {
    let surveyStore = store.getNamespacedStore("survey");

    this.currentIndex = surveyStore.get("currentIndex") || 0;
    // this.isFinished = surveyStore.get("isFinished") || false;

    this.finished = finished;
    this.openQuestionaire = openQuestionaire;
    this.back = back;
    this.next = next;

    function finished() {
        // this.isFinished = true;
        // surveyStore.set("isFinished", this.isFinished);
    }

    function openQuestionaire() {
        ga("send", "event", "Questionaire", "opened");
    }

    function back() {
        if (this.currentIndex < 1)
            return;

        this.currentIndex--;
        surveyStore.set("currentIndex", this.currentIndex);
    }

    function next() {
        if (this.currentIndex >= this.exercises.length - 1)
            return;


        this.currentIndex++;
        surveyStore.set("currentIndex", this.currentIndex);

        // Google Analytics Events
        if( this.currentIndex === 1)
            ga("send", "event", "Tutorial", "started");
        if( this.currentIndex === this.exercises.length - 1)
            ga("send", "event", "Tutorial", "finished");
    }

    this.exercises = [
        {
            title: "Willkommen bei Wust",
            secondTitle: "Fahren Sie mit der Maus über dieses Feld",
            email: true,
            description: `
**Wust** ist ein Graph-basiertes Diskussionsystem.

- Bitte führen Sie alle hier gestellten Aufgaben aus, um mit dem System vertraut zu werden.
- Sie entscheiden selbst, wann eine Aufgabe erledigt ist.
- Am Ende der Aufgaben finden Sie einen Link zu einem Fragebogen. Wir würden uns freuen, wenn sie diesen nach den Aufgaben ausfüllen würden.
- Die erste Aufgabe sehen Sie mit einem Klick auf **Weiter**.
`
        },
        {
            title: "Registrieren Sie sich",
            description: `
1. Klicken Sie rechts oben auf das Menü **Register / Login**.
1. Registrieren Sie einen Benutzer-Account.

Wenn Sie bereits einen Account haben, können Sie sich auch damit einloggen.
`
        },
        {
            title: "Betrachten Sie einen Beitrag",
            description: `
1. Klicken Sie in der Navigationsleiste auf **Dashboard**. Hier sehen Sie neue Beiträge - dargestellt als farbige Boxen.
1. Klicken Sie auf einen beliebigen Beitrag, um ihn in der Fokus-Ansicht zu lesen.
`
        },
        {
            title: "Stellen Sie eine Frage zum Beitrag",
            description: `
1. Klicken Sie auf **Respond**, um eine Frage zu diesem Beitrag zu stellen.
1. Wenn Ihnen keine passende Frage einfällt, stellen Sie eine beliebige Frage.
1. Im zweiten Eingabefeld können Sie auswählen, wie sich Ihr Beitrag auf den anderen bezieht (Runde Boxen). Wählen Sie bitte **Question** aus.
1. Bestätigen Sie rechts unten mit **Respond**.

In der Spalte **rechts** neben dem Beitrag sehen Sie Beiträge, die sich auf diesen beziehen.
`
        },
        {
            title: "Navigieren Sie zu Ihrer Frage und zurück",
            description: `
1. Klicken Sie auf Ihre soeben erstellte Frage.
1. Auf der **linken Seite** sehen Sie Beiträge, auf die sich dieser Beitrag bezieht.
1. Klicken Sie wieder auf den ursprünglichen Beitrag.
`
        },{
            title: "Wechseln Sie in die Graph-Ansicht",
            description: `
1. Klicken Sie nun auf der linken Seite auf das Symbol ![graph-icon](assets/images/icon-graph.png), um in die Graph-Ansicht zu wechseln.
1. Klicken Sie auf das **+** Symbol über dem ursprünglichen Beitrag und stellen Sie noch eine Frage.
1. Fügen Sie eine Idee zu Ihrer gestellten Frage hinzu.
1. Das zweite Symbol über den Beiträgen lässt Sie den Beitrag durch Ziehen mit anderen verbinden. Verbinden Sie die Idee mit der anderen Frage und klassifizieren Sie die Verbindung als Idee.
1. Experimentieren Sie ein bisschen. Verbinden Sie beispielsweise einen beliebigen Beitrag mit einer Verbindung.
`
        },
        {
            title: "Starten Sie eine neue Diskussion",
            description: `
1. Starten Sie eine neue Diskussion indem Sie oben in der Navigationsleiste auf den Knopf **Start Discussion** klicken. Wenn Ihnen nichts einfällt: Nehmen Sie an, Sie hätten ein Problem in Ihrem Haushalt und möchten andere Leute um Hilfe bitten.
1. Wählen Sie im zweiten Eingabefeld einen passenden Kontext (eckige Boxen) aus, z.B. Haushalt. Um einen neuen Kontext zu erzeugen, geben Sie den Namen ein und bestätigen Sie mit ENTER.
1. Veröffentlichen Sie die neue Diskussion mit einem Klick auf **Start Discussion**.

Es gibt 2 unterschiedliche **Tag**-Arten: **Klassifikationen** (Frage, Idee, ...) und **Kontexte** (Themengebiete wie Haushalt, Politik). Klassifikationen gelten immer im Bezug auf einen anderen Beitrag oder einen Kontext. **Neue Diskussionen müssen mindestens einen Kontext haben**. Definieren Sie einen oder auch mehrere Kontexte, damit Ihr Beitrag besser gefunden werden kann.
`
        },
        {
            title: "Bearbeiten Sie einen fremden Beitrag",
            description: `
1. Gehen Sie zum **Dashboard** und sehen Sie sich einen weiteren fremden Beitrag in der Fokus-Ansicht an.
1. Klicken Sie rechts oben auf den Knopf mit dem Stift-Symbol, um den Beitrag zu bearbeiten.
1. Ändern Sie etwas in der Beschreibung des Beitrags.
1. Beachten Sie den Fortschrittsbalken über dem **Save**-Knopf.

Wenn Sie etwas an einem fremden Beitrag verändern, müssen andere Nutzer der Änderung zustimmen. Die Anzahl der nötigen Stimmen hängt davon ab, wie oft der Beitrag angesehen wurde.

Für akzeptierte Änderungen erhalten sie **Karma**-Punkte in dem jeweiligen Kontext (Themenbereich). Damit haben Sie mehr Einfluss in der Abstimmung über Änderungen in diesem Kontext.
`
        },
        {
            title: "Bewerten Sie Änderungen anderer Nutzer",
            description: `
1. Klicken Sie in der Navigationsleiste auf den Eintrag **Moderation**. Hier können Sie über vorgeschlagene Änderungen anderer Nutzer abstimmen. Sie entscheiden mit, welche Änderungen akzeptiert und welche abgelehnt werden.
1. Stimmen Sie über ein oder zwei Änderungen ab.

Wurden viele Ihrer eigenen Änderungsvorschläge akzeptiert, haben Sie mehr **Karma**-Punkte und damit mehr Stimmgewicht. In einigen Fällen führt das dazu, dass Ihre Änderungen sofort sichtbar sind - Nutzer können dann nachträglich darüber abstimmen. Wird eine Änderung abgelehnt, so wird sie automatisch rückgängig gemacht.
`
        },
        {
            title: "Bitte füllen Sie den Fragebogen aus",
            email: true,
            description: `
Vielen Dank, Sie haben alle Aufgaben erledigt.

Schauen Sie sich nun weiter im System um. Wenn Sie möchten, können Sie gerne weitere Diskussionen starten und sich an anderen beteiligen, bevor Sie den Fragebogen ausfüllen.
`
        }
    ];
}

function survey() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "elements/survey/survey.html",
        scope: {
            canHide: "@"
        },
        controller: surveyCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

surveyCtrl.$inject = ["SurveyService"];

function surveyCtrl(SurveyService) {
    let vm = this;

    vm.survey = SurveyService;

}
