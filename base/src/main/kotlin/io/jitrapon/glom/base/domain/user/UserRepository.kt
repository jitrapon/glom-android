package io.jitrapon.glom.base.domain.user

import android.support.v4.util.ArrayMap
import io.jitrapon.glom.base.repository.Repository
import io.reactivex.Flowable

/**
 * Repository for retrieving and saving User information
 *
 * @author Jitrapon Tiachunpun
 */
class UserRepository(private val remoteDataSource: UserDataSource) : Repository<User>(), UserDataSource {

    /* in-memory cache of the list of users */
    private var users: List<User>? = null
    
    /* needed so that we can retrieve a User object by their ID in O(1) time */
    private var userMap: ArrayMap<String, User> = ArrayMap()

    override fun getUsers(circleId: String, refresh: Boolean): Flowable<List<User>> {
        users = users ?: getItems()
        return loadList(refresh,
                Flowable.just(users),
                remoteDataSource.getUsers(circleId, refresh),
                {
                    users = it
                    userMap = userMap.apply {
                        users?.map {
                            put(it.userId, it)
                        }
                    }
                    users!!
                }
        )
    }

    override fun getUsers(userIds: List<String>): Flowable<List<User>> {
        return Flowable.just(ArrayList<User>().apply {
            userIds.forEach {
                userMap[it]?.let(::add)
            }
        })
    }

    private fun getItems(): List<User> {
        return ArrayList<User>().apply {
            add(User(User.TYPE_USER, "yoshi3003", "boat", "https://yt3.ggpht.com/-Dqv4xtV9L48/AAAAAAAAAAI/AAAAAAAAAAA/bDw66DjBn10/s900-c-k-no-mo-rj-c0xffffff/photo.jpg"))
            add(User(User.TYPE_USER, "fatcat18", "nad", "https://lh3.googleusercontent.com/-pvVb9ECpAt8/AAAAAAAAAAI/AAAAAAAAAww/_bIFybG8kk8/s60-p-rw-no/photo.jpg"))
            add(User(User.TYPE_USER, "fluffy", "fluffy", "https://rodtank.files.wordpress.com/2015/06/05f26a_3bd838d073dd4c3e9519cd2f09d07fb6_srz_p_465_333_75_22_0-50_1-20_0-00_jpg_srz.jpg?w=382&h=274"))
            add(User(User.TYPE_USER, "panda", "panda", null))
        }
    }
}
