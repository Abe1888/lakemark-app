package com.example.data.auth

import com.example.data.model.UserRole
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class HashedStaffPassword(
    val saltHex: String,
    val hashHex: String,
    val hashAlgo: String = StaffPasswordHasher.ALGO_PBKDF2_SHA256,
)

data class StarterStaffAccount(
    val uid: String,
    val name: String,
    val username: String,
    val role: UserRole,
    val initialPassword: String,
    val waiterAreas: List<String> = emptyList(),
)

object DefaultStaffRoster {
    val accounts: List<StarterStaffAccount> = listOf(
        StarterStaffAccount(
            uid = "staff_reception1",
            name = "Hanna Tadesse",
            username = "reception1",
            role = UserRole.RECEPTION,
            initialPassword = "Lakemark#101",
        ),
        StarterStaffAccount(
            uid = "staff_kitchen1",
            name = "Chef Dawit",
            username = "kitchen1",
            role = UserRole.KITCHEN,
            initialPassword = "Lakemark#102",
        ),
        StarterStaffAccount(
            uid = "staff_waiter1",
            name = "Abel Tesfaye",
            username = "waiter1",
            role = UserRole.WAITER,
            initialPassword = "Lakemark#103",
            waiterAreas = listOf("f1", "din"),
        ),
        StarterStaffAccount(
            uid = "staff_cashier1",
            name = "Sara Mekonnen",
            username = "cashier1",
            role = UserRole.CASHIER,
            initialPassword = "Lakemark#104",
        ),
        StarterStaffAccount(
            uid = "staff_delivery1",
            name = "Bereket Alemu",
            username = "delivery1",
            role = UserRole.DELIVERY,
            initialPassword = "Lakemark#105",
        ),
    )
}

object StaffPasswordHasher {
    const val ALGO_PBKDF2_SHA256 = "pbkdf2_sha256"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 512
    private val secureRandom = SecureRandom()

    fun randomSaltHex(): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        return saltBytes.toHex()
    }

    fun hashPassword(password: String, saltHex: String = randomSaltHex()): HashedStaffPassword {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            password.toCharArray(),
            saltHex.toByteArray(Charsets.UTF_8),
            ITERATIONS,
            KEY_LENGTH_BITS,
        )
        return try {
            val encoded = factory.generateSecret(spec).encoded
            HashedStaffPassword(
                saltHex = saltHex,
                hashHex = encoded.toHex(),
                hashAlgo = ALGO_PBKDF2_SHA256,
            )
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { b ->
        ((b.toInt() and 0xFF) + 0x100).toString(16).substring(1)
    }
}
