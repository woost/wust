 angular.module("wust").config(function(restmodProvider) {
     restmodProvider.rebase({
         $config: {
             urlPrefix: "/api/v1"
         }
     });
     restmodProvider.rebase("AMSApi");
 });
