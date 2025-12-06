package com.example.pockettherapist

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object UserStore {
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")

    var loggedInUser: String? = null
    var age: String? = null
    var gender: String? = null

    fun signUp(
        username: String,
        password: String,
        age: String,
        gender: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        usersRef.child(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    onFailure("Username already taken")
                } else {
                    val userData = mapOf(
                        "password" to password,
                        "age" to age,
                        "gender" to gender
                    )
                    usersRef.child(username).setValue(userData)
                        .addOnSuccessListener {
                            loggedInUser = username
                            this@UserStore.age = age
                            this@UserStore.gender = gender
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onFailure(e.message ?: "Sign up failed")
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error.message)
            }
        })
    }

    fun signIn(
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        usersRef.child(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    onFailure("User not found")
                    return
                }

                val storedPassword = snapshot.child("password").getValue(String::class.java)
                if (storedPassword == password) {
                    loggedInUser = username
                    age = snapshot.child("age").getValue(String::class.java)
                    gender = snapshot.child("gender").getValue(String::class.java)
                    onSuccess()
                } else {
                    onFailure("Incorrect password")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error.message)
            }
        })
    }

    fun updateProfile(
        age: String,
        gender: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val username = loggedInUser ?: run {
            onFailure("No user logged in")
            return
        }

        val updates = mapOf(
            "age" to age,
            "gender" to gender
        )

        usersRef.child(username).updateChildren(updates)
            .addOnSuccessListener {
                this@UserStore.age = age
                this@UserStore.gender = gender
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Update failed")
            }
    }

    fun signOut() {
        loggedInUser = null
        age = null
        gender = null
    }
}
