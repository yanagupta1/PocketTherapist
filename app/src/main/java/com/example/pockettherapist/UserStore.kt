package com.example.pockettherapist

object UserStore {
    private val users = mutableMapOf<String, String>() // username -> password
    var loggedInUser: String? = null

    var age: String? = null
    var gender: String? = null

    fun signUp(username: String, password: String): Boolean {
        if (users.containsKey(username)) return false
        users[username] = password
        return true
    }

    fun signIn(username: String, password: String): Boolean {
        val correct = users[username] == password
        if (correct) loggedInUser = username
        return correct
    }

    fun signOut() {
        loggedInUser = null
    }
}
