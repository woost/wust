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
            title: "Willkommen bei Wust!",
            description: `
Fahren Sie mit der Maus über dieses Feld.

**Wust** ist ein Graph-basiertes Diskussionsystem.

Bitte führen Sie alle hier gestellten Aufgaben aus, um mit dem System vertraut zu werden. Die erste Aufgabe sehen Sie mit einem Klick auf **Weiter**.

Bei Fragen oder Problemen schreiben Sie uns bitte eine kurze E-Mail:
<felix.dietze@rwth-aachen.de>
<johannes.karoff@rwth-aachen.de>
`
        },
        {
            title: "Fokus-Ansicht eines Beitrags",
            description: `
Fahren Sie mit der Maus über dieses Feld.

**Wust** ist ein Graph-basiertes Diskussionsystem.

Bitte führen Sie alle hier gestellten Aufgaben aus, um mit dem System vertraut zu werden. Die erste Aufgabe sehen Sie mit einem Klick auf \`\`\`Weiter\`\`\`.

Bei Fragen oder Problemen schreiben Sie uns bitte eine kurze E-Mail:
<felix.dietze@rwth-aachen.de>
<johannes.karoff@rwth-aachen.de>
`
        },
        {
            title: "Sie haben alle Aufgaben Erledigt",
            description: `Bitte füllen Sie jetzt den [Fragebogen](https://www.surveymonkey.com/r/woost) aus`
        }
    ];

    vm.next = next;
    vm.back = back;
    vm.finished = finished;

    function finished() {
        surveyStore.set("currentIndex", undefined);
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
