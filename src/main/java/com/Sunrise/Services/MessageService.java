package com.Sunrise.Services;

import com.Sunrise.DTO.DBResults.MessageResult;
import com.Sunrise.DTO.ServiceResults.CreateMessageResult;
import com.Sunrise.DTO.ServiceResults.GetChatMessagesResult;
import com.Sunrise.DTO.ServiceResults.SimpleResult;
import com.Sunrise.DTO.ServiceResults.VisibleMessagesCountResult;
import com.Sunrise.Services.DataServices.DataAccessService;
import com.Sunrise.Subclasses.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    private final DataAccessService dataAccessService;
    private final LockService lockService;

    public MessageService(DataAccessService dataAccessService, LockService lockService){
        this.dataAccessService = dataAccessService;
        this.lockService = lockService;
    }

    public CreateMessageResult createPublicMessage(Long chatId, Long userId){
        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validateUserInChat(chatId, userId);

            //

            // и возврат успеха
        }
        catch (ValidationException e) {
            return CreateMessageResult.error(e.getMessage());
        }
        catch (Exception e) {
            return CreateMessageResult.error("GetChatMessages failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
        }
    }
    public CreateMessageResult createPrivateMessage(Long chatId, Long userId, String text){

    }

    public GetChatMessagesResult getChatMessages(Long chatId, Long userId, Integer limit, Integer offset) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validateUserInChat(chatId, userId);

            List<MessageResult> messages = dataAccessService.getChatMessages(chatId, userId, limit, offset);

            return GetChatMessagesResult.success(messages);
        }
        catch (ValidationException e) {
            return GetChatMessagesResult.error(e.getMessage());
        }
        catch (Exception e) {
            return GetChatMessagesResult.error("GetChatMessages failed due to server error");
        }
        finally {
            lockService.unlockReadChat(chatId);
        }
    }
    public VisibleMessagesCountResult getVisibleMessagesCount(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validateUserInChat(chatId, userId);

            int count = dataAccessService.getVisibleMessagesCount(chatId, userId);

            return new VisibleMessagesCountResult(true, null, count);
        }
        catch (ValidationException e) {
            return VisibleMessagesCountResult.error(e.getMessage());
        }
        catch (Exception e) {
            return VisibleMessagesCountResult.error("GetVisibleMessagesCount failed due to server error");
        }
        finally{
            lockService.unlockReadChat(chatId);
        }
    }

    public SimpleResult markMessageAsRead(Long chatId, Long messageId, Long userId) {

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ЧАТ
        try
        {
            validateUserInChat(chatId, userId);

            dataAccessService.markMessageAsRead(messageId, userId); // уведомить всех надо об этом

            return SimpleResult.success();
        }
        catch (ValidationException e) {
            return SimpleResult.error(e.getMessage());
        }
        catch (Exception e) {
            return SimpleResult.error("MarkMessageAsRead failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
        }
    } // Надо всех уведомить




    private void validateUserInChat(Long chatId, Long userId) {
        if (!dataAccessService.existsChat(chatId))
            throw new ValidationException("Chat not found");

        if (dataAccessService.notExistsUserById(userId))
            throw new ValidationException("User not found");

        if (!dataAccessService.isUserInChat(chatId, userId))
            throw new ValidationException("User is not a member of this chat");
    }
}
