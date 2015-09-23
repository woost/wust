require 'airborne'

Airborne.configure do |config|
    config.base_url =  'http://localhost:9000/api/v1'
    config.headers = { 'x-auth-token' => x_token }
end

describe 'bla' do
    it 'no unauthorized create post' do
        # http://www.rubydoc.info/gems/json_web_token/0.3.1
        post '/posts', { title: 'hallo' }, { 'x-auth-token' => '...' }
        expect_status(400)
    end

    it 'create post' do
        # http://www.rubydoc.info/gems/json_web_token/0.3.1
        post '/posts', { title: 'hallo' }, { 'x-auth-token' => '...' }
        expect_json_types(title: :string)
        expect_json(title: 'hallo')
    end

    it 'create post' do
        # http://www.rubydoc.info/gems/json_web_token/0.3.1
        post '/posts', { title: 'hallo' }, { 'x-auth-token' => '...' }
        expect_json_types(title: :string)
        expect_json(title: 'hallo')
    end

    # it 'list posts' do
    #     get 'http://localhost:9000/api/v1/posts'
    #     expect_json_types(title: :string)
    # end
end
