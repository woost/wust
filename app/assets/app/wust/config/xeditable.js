angular.module("wust").run(run);

run.$inject = ["editableOptions"];

function run(editableOptions) {
    // http://vitalets.github.io/angular-xeditable/#overview
    editableOptions.theme = "bs3";
}
