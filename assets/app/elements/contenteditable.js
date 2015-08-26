angular.module("wust.elements").directive("contenteditable", contenteditable);

contenteditable.$inject = [];

function contenteditable() {
  return {
    require: "ngModel",
    link: function(scope, element, attrs, ngModel) {
        //TODO: does not handle copy paste
        if (attrs.maxchars) {
            element.bind("keydown", function(e) {
                var keycode = e.keyCode;

                //List of keycodes of printable characters from:
                //http://stackoverflow.com/questions/12467240/determine-if-javascript-e-keycode-is-a-printable-non-control-character
                var printable =
                    (keycode > 47 && keycode < 58)   || // number keys
                    keycode === 32 || keycode === 13   || // spacebar & return key(s) (if you want to allow carriage returns)
                    (keycode > 64 && keycode < 91)   || // letter keys
                    (keycode > 95 && keycode < 112)  || // numpad keys
                    (keycode > 185 && keycode < 193) || // ;=,-./` (in order)
                    (keycode > 218 && keycode < 223);   // [\]' (in order)

                let text = element.text();
                if (printable) {
                    if (text.length >= attrs.maxchars) {
                        e.preventDefault();
                        return;
                    }
                }
            });
        }

        ngModel.$render = function() {
            element.text(ngModel.$viewValue || "");
        };

        element.bind("blur keyup change", function() {
            scope.$apply(() => ngModel.$setViewValue(element.text()));
        });
    }
  };
}
