-- :name save-message! :! :n
-- :doc creates a new message using name and message keys
INSERT INTO posts
(name, message)
VALUES (:name, :message)

-- :name get-messages :? :*
-- :doc selects all available messages
SELECT * from posts

-- :name create-user!* :! :n
-- :doc creates a new user with provided login and hashed password
INSERT INTO users
(login, password)
VALUES (:login, :password)

-- :name get-user-for-auth* :? :1
-- :doc selects a user for authentication
SELECT * from users WHERE login = :login
