Rails.application.routes.draw do
  namespace :api, defaults: { format: :json } do
    resources :graphs, only: [:show]
  end

  get '*path' => 'main_view#index'
  root 'main_view#index'
end
