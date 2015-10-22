angular.module("wust.elements").directive("survey", survey);

survey.$inject = [];

function survey() {
    return {
        restrict: "A",
        replace: true,
        templateUrl: "elements/survey/survey.html",
        scope: true,
        controller: surveyCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

surveyCtrl.$inject = ["store"];

function surveyCtrl(store) {
    let vm = this;
    let surveyStore = store.getNamespacedStore("survey");

    vm.currentIndex = surveyStore.get("currentIndex") || 0;
    vm.exercises = [
        {
            title: "Willkommen bei Wust",
            secondTitle: "Fahren Sie mit der Maus über dieses Feld",
            email: true,
            description: `
**Wust** ist ein Graph-basiertes Diskussionsystem.

- Bitte führen Sie alle hier gestellten Aufgaben aus, um mit dem System vertraut zu werden.
- Sie entscheiden selbst, wann eine Aufgabe erledigt ist.
- Die erste Aufgabe sehen Sie mit einem Klick auf **Weiter**.
`
        },
        {
            title: "Registrieren Sie sich",
            description: `
- Klicken Sie rechts oben auf das Menü **Register / Login**.
- Registrieren Sie einen Benutzer-Account.
- Wenn Sie bereits einen Account haben, können Sie sich auch damit einloggen.
`
        },
        {
            title: "Betrachten Sie einen Beitrag",
            description: `
1. Klicken Sie in der Navigationsleiste auf **Dashboard**. Hier sehen Sie neue Beiträge - dargestellt als farbige Boxen.
1. Klicken Sie auf einen beliebigen Beitrag um in die Focus-Ansicht des Beitrags zu wechseln.
`
        },
        {
            title: "Stellen Sie eine Frage zum Beitrag",
            description: `
Auf der **rechten Seite** sehen Sie Beiträge, die sich auf diesen Beitrag beziehen.

1. Klicken Sie auf **Respond**, um eine Frage zu diesem Beitrag zu stellen.
1. Wenn Ihnen keine passende Frage einfällt, stellen Sie eine beliebige Frage.
1. Im zweiten Eingabefeld können Sie auswählen, wie sich Ihr Beitrag auf den anderen bezieht (Runde Boxen). Wählen Sie bitte **Question** aus.
1. Klicken Sie rechts unten auf **Respond**.
`
        },
        {
            title: "Navigieren Sie zu Ihrer Frage und zurück",
            description: `
- Klicken Sie auf ihre soeben erstellte Frage.
- Auf der **linken Seite** sehen Sie Beiträge, auf die sich dieser Beitrag bezieht.
- Klicken Sie wieder auf den ursprünglichen Beitrag.
`
        },{
            title: "Wechseln Sie in die Graph-Ansicht",
            description: `
1. Klicken Sie nun auf der linken Seite auf das Symbol ![graph-icon](assets/images/icon-graph.png), um in die Graph-Ansicht zu wechseln.
1. Klicken Sie auf das **+** Symbol über dem ursprünglichen Beitrag und stellen Sie noch eine Frage.
1. Fügen Sie eine Idee zu Ihrer gestellten Frage hinzu.
1. Das zweite Symbol über den Knoten lässt Sie den Beitrag durch Ziehen mit anderen verbinden. Verbinden Sie die Idee mit der anderen Frage.
1. Verbinden Sie einen Beitrag mit einer Verbindung.
`
        },
        {
            title: "Starten Sie eine neue Diskussion",
            description: `
1. Starten Sie eine neue Diskussion indem Sie auf den Knopf **Start Discussion** oben in der Navigationsleiste klicken, z.B., Sie haben ein Problem in ihrem Haushalt und möchten andere Leute nach Ideen fragen.
1. Wählen Sie im zweiten Eingabefeld bitte den Kontext **Haushalt** aus (Eckige Boxen).
1. Es gibt 2 unterschiedliche Tag-Arten: **Klassifikationen** (Frage, Idee, ...) und **Kontexte** (Themengebiete wie Haushalt, Politik). Klassifikationen gelten immer im Bezug auf einen anderen Beitrag oder einen Kontext. **Neue Diskussionen müssen mindestens einen Kontext haben**.
`
        },
        {
            title: "Bearbeiten Sie einen fremden Beitrag",
            description: `
1. Sehen Sie sich einen weiteren fremden Beitrag in der Focus-Ansicht an.
1. Klicken Sie rechts oben auf den Knopf mit dem Stift-Symbol, um den Beitrag zu bearbeiten.
1. Ändern sie etwas in der Beschreibung des Beitrags.
1. Beachten Sie den Fortschrittsbalken über dem "Save"-Knopf.

Wenn Sie etwas an einem fremden Beitrag verändern, müssen andere Nutzer der Änderung zustimmen. Die Anzahl der nötigen Stimmen hängt davon ab, wie oft der Beitrag angesehen wurde.
`
        },
        {
            title: "Bewerten Sie Änderungen anderer Nutzer",
            description: `
1. Klicken Sie in der Navigationsleiste auf den Eintrag **Moderation**. Hier können Sie über vorgeschlagene Änderungen anderer Nutzer abstimmen. Sie entscheiden mit, welche Änderungen akzeptiert und welche abgelehnt werden.
1. Stimmen Sie über ein oder zwei Änderungen ab.

Wurden viele Ihrer Änderungsvorschläge akzeptiert, haben Sie mehr Stimmen. In einigen Fällen führt das dazu, dass Ihre Änderungen sofort sichtbar sind - Nutzer können dann nachträglich darüber abstimmen. Wird eine Änderung abgelehnt, so wird sie automatisch rückgängig gemacht.
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

    // TODO: Seitenzahlen

    vm.next = next;
    vm.back = back;
    vm.finished = finished;

    function finished() {
        // surveyStore.set("currentIndex", undefined);
    }

    function back() {
        if (vm.currentIndex < 1)
            return;

        vm.currentIndex--;
        surveyStore.set("currentIndex", vm.currentIndex);
    }

    function next() {
        if (vm.currentIndex >= vm.exercises.length - 1)
            return;

        vm.currentIndex++;
        surveyStore.set("currentIndex", vm.currentIndex);
    }
}
