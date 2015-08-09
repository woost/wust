angular.module("wust.elements").directive("infiniteScroll", infiniteScroll);

infiniteScroll.$inject = ["$rootScope"];

function infiniteScroll($rootScope) {
    return {
        scope: {
            infiniteScroll: "&",
            promise: "=",
            infinite: "="
        },
        link
    };

    //TODO: refactor
    function link(scope, elem, attrs) {
        scope.infinite = {
            manualLoad, initialize
        };

        let atPercent = attrs.atPercent || 80;
        let targetElement = attrs.scrollTarget ? document.getElementById(attrs.scrollTarget) : elem[0];
        let lastScrollTop = 0;
        let pageInfos;
        initialize();

        targetElement.addEventListener("scroll", function () {
            assurePageInfo();
            let scrollHeight = targetElement.scrollHeight;
            let scrollTop = targetElement.scrollTop;
            let scrollBottom = scrollTop + targetElement.offsetHeight;
            if (!scope.infinite.loading && !scope.infinite.noMore && scrollTop > lastScrollTop) {
                let percent = Math.abs((scrollBottom / scrollHeight) * 100);
                if (percent >= atPercent) {
                    callLoader(scrollHeight);
                }
            }

            setPage(scrollBottom);
            lastScrollTop = scrollTop;
            scope.$apply();
        });

        function callLoader(height) {
            scope.infinite.loading = true;
            let result = scope.infiniteScroll();
            if (result) {
                scope.promise.$then(({$response: {config, data}}) => {
                    scope.infinite.loading = false;
                    if (data.length === 0) {
                        scope.infinite.noMore = true;
                    } else {
                        addPage(height);
                        setPage(targetElement.scrollTop + targetElement.offsetHeight);
                        scope.infinite.noMore = data.length < config.params.size;
                    }

                    if (scope.infinite.noMore) {
                        scope.infinite.maxPage = pageInfos.length - 1;
                    }
                }, () => scope.infinite.loading = false);
            } else {
                scope.infinite.noMore = false;
            }
        }

        function initialize() {
            scope.infinite.currentPage = 0;
            scope.infinite.maxPage = -1;
            scope.infinite.noMore = true;
            scope.infinite.loading = true;
            pageInfos = [];

            if (scope.promise.$then) {
                scope.promise.$then(({$response}) => {
                    scope.infinite.loading = false;
                    if ($response === undefined)
                        return;

                    let config = $response.config;
                    let data = $response.data;
                    scope.infinite.noMore = data.length === 0 || data.length < config.params.size;
                    if (scope.infinite.noMore) {
                        scope.infinite.maxPage = 0;
                    }
                }, () => scope.infinite.loading = false);
            } else {
                scope.infinite.loading = false;
            }
        }

        function assurePageInfo() {
            if (pageInfos.length === 0)
                addPage(targetElement.scrollTop);
        }

        function manualLoad() {
            if (scope.infinite.noMore)
                return;

            assurePageInfo();
            callLoader(targetElement.scrollHeight);
        }

        function setPage(pos) {
            scope.infinite.currentPage = 0;
            for (let i = 0;  i < pageInfos.length; i++) {
                let curr = pageInfos[i];
                if (curr <= pos)
                    scope.infinite.currentPage = i;
                else
                    break;
            }
        }

        function addPage(pos) {
            pageInfos.push(pos);
        }
    }
}
