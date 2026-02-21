package com.echo.lutian.data.dao

import androidx.room.*
import com.echo.lutian.data.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * 用户数据访问对象
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM users ORDER BY lastActiveAt DESC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE deviceId = :deviceId")
    suspend fun getUserByDeviceId(deviceId: String): User?

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): User?

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET isCurrentUser = 0")
    suspend fun clearCurrentUser()

    @Query("UPDATE users SET isCurrentUser = 1 WHERE userId = :userId")
    suspend fun setCurrentUser(userId: String)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
