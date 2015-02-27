module Api
  class GraphsController < ApplicationController
    respond_to :json

    def show
      graph = Graph.all.find { |g| g[:id] == params[:id].to_i }
      if graph
        render json: { graph:  graph }, status: :ok
      else
        render json: {}, status: :unprocessable_entity
      end
    end

    def index
      render json: { graphs: Graph.all }, status: :ok
    end
  end
end
