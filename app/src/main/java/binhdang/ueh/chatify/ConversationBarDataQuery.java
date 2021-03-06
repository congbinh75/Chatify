package binhdang.ueh.chatify;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Size;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConversationBarDataQuery {
    Context context;
    List<ConversationBar> data = new ArrayList<ConversationBar>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    SharedPreferences sharedPref;

    public ConversationBarDataQuery(Context context){
        this.context = context;
        retrieveData();
    }

    public void retrieveData(){
        data.clear();
        sharedPref = context.getSharedPreferences(BuildConfig.APPLICATION_ID, MODE_PRIVATE);
        //LEVEL 1 - Check which conversations you are in
        //-------------------------------------------------------------
        db.collection("conversationsMembers")
                .whereEqualTo("member", sharedPref.getString("username", ""))
                .get()
                .addOnCompleteListener(task -> {
                    for (QueryDocumentSnapshot document : task.getResult()){
                        if (document.getData().size() > 0) {
                            Map result = document.getData();
                            // LEVEL 2 - Get the type of conversation
                            //-------------------------------------------------------------
                            db.collection("conversations")
                                    .whereEqualTo("conversation", result.get("conversation").toString())
                                    .get()
                                    .addOnCompleteListener(task1 -> {
                                        for (QueryDocumentSnapshot document1 : task1.getResult()) {
                                            if (document1.getData().size() > 0) {
                                                Map result1 = document1.getData();
                                                if (result1.get("type").equals("private")) {
                                                    // LEVEL 3 - Get conversation bar details if conversation is private
                                                    //-------------------------------------------------------------
                                                    db.collection("conversationsBars")
                                                            .whereEqualTo("conversation", result1.get("conversation"))
                                                            .get()
                                                            .addOnCompleteListener(task2 -> {
                                                                for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                                    if (document2.getData().size() > 0) {
                                                                        Map result2 = document2.getData();
                                                                        if (result2.get("otherUser").toString().equals(sharedPref.getString("username", ""))) {
                                                                            continue;
                                                                        }
                                                                        // LEVEL 4 - Add details to bar after retrieve other user's data
                                                                        //-------------------------------------------------------------
                                                                        db.collection("users")
                                                                                .whereEqualTo("username", result2.get("otherUser"))
                                                                                .get()
                                                                                .addOnCompleteListener(task3 -> {
                                                                                    for (QueryDocumentSnapshot document3 : task3.getResult()) {
                                                                                        if (document3.getData().size() > 0) {
                                                                                            Map result3 = document3.getData();
                                                                                            ConversationBar bar = new ConversationBar(
                                                                                                    document2.getId(),
                                                                                                    document2.get("conversation").toString(),
                                                                                                    result3.get("displayName").toString(),
                                                                                                    "pfp/" + result3.get("username") + ".jpg",
                                                                                                    result2.get("lastConversationMsg").toString(),
                                                                                                    result2.get("lastConversationTime").toString());
                                                                                            data.add(bar);
                                                                                        }
                                                                                    }
                                                                                    Tasks.whenAllSuccess(task, task1, task2, task3).addOnSuccessListener(list -> MainActivity.getInstanceActivity().UpdateMenu());
                                                                                });
                                                                    }
                                                                }
                                                            });
                                                } else {

                                                }
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    public List<ConversationBar> getData(){
        return data;
    }
}
