package chat.rocket.android.pinnedmessages.presentation

import chat.rocket.android.chatroom.viewmodel.ViewModelMapper
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.server.domain.GetChatRoomsInteractor
import chat.rocket.android.server.domain.GetCurrentServerInteractor
import chat.rocket.android.server.infraestructure.RocketChatClientFactory
import chat.rocket.android.util.extensions.launchUI
import chat.rocket.common.RocketChatException
import chat.rocket.common.util.ifNull
import chat.rocket.core.internal.rest.getPinnedMessages
import chat.rocket.core.model.isSystemMessage
import timber.log.Timber
import javax.inject.Inject

class PinnedMessagesPresenter @Inject constructor(
    private val view: PinnedMessagesView,
    private val strategy: CancelStrategy,
    private val serverInteractor: GetCurrentServerInteractor,
    private val roomsInteractor: GetChatRoomsInteractor,
    private val mapper: ViewModelMapper,
    factory: RocketChatClientFactory
) {
    private val client = factory.create(serverInteractor.get()!!)
    private var pinnedMessagesListOffset: Int = 0

    /**
     * Load all pinned messages for the given room id.
     *
     * @param roomId The id of the room to get pinned messages from.
     */
    fun loadPinnedMessages(roomId: String) {
        launchUI(strategy) {
            try {
                val serverUrl = serverInteractor.get()!!
                val chatRoom = roomsInteractor.getById(serverUrl, roomId)
                chatRoom?.let { room ->
                    view.showLoading()
                    val pinnedMessages =
                        client.getPinnedMessages(roomId, room.type, pinnedMessagesListOffset)
                    pinnedMessagesListOffset = pinnedMessages.offset.toInt()
                    val messageList = mapper.map(pinnedMessages.result.filterNot { it.isSystemMessage() })
                    view.showPinnedMessages(messageList)
                    view.hideLoading()
                }.ifNull {
                    Timber.e("Couldn't find a room with id: $roomId at current server.")
                }
            } catch (e: RocketChatException) {
                Timber.e(e)
            }
        }
    }
}