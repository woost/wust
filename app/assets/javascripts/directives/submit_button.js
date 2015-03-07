app.directive("submitOnClick", function($parse) {
    return {
        compile: function(element, attrs) {
            var exp = $parse("($this).parent('form').submit()");
            return function(scope, elem) {
                elem.bind('click', function() {
                    exp(scope);
                });
            };
        }
    };
});
