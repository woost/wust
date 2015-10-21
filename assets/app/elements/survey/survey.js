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
            title: "Do something",
            description: `
Should be a cool thing

# foo
1. bar
2. baz
            `
        },
        {
            title: "Vielen Dank für deine Teilnahme!",
            description: "Bitte fülle jetzt den Fragebogen aus"
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
