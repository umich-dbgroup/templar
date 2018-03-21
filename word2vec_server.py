import gensim
import socket
import threading

def get_n_sim(model, words1, words2):
    return round(model.wv.n_similarity(words1, words2), 4)

def filter_words(model, words):
    result = []
    num_del = 0
    for word in words:
        if word in model.wv:
            result.append(word)
        else:
            num_del += 1
    return result, num_del

def listen_to_connection(model, connection, address):
    sent = False
    while True:
        try:
            print("connection from ", address)
            data = connection.recv(1024)
            words = data.decode('utf-8').strip().split(", ")
            print(words[0], words[1])

            words1, num_del1 = filter_words(model, words[0].split(" "))
            words2, num_del2 = filter_words(model, words[1].split(" "))

            score = 0
            if len(words1) == 0 or len(words2) == 0:
                print("One of the token lists is empty. Return -1")
                score = -1
            else:
                score = get_n_sim(model, words1, words2)
                # if anything is deleted, enact penalty
                score -= 0.000001 * (num_del1 + num_del2)
                print("score:", score)
            connection.send(str.encode(str(score)))
            sent = True
        except Exception, e:
            print "Unexpected error:", str(e)
            print("no score. sending -1")
            if not sent:
                connection.send(str.encode(str(-1)))
        finally:
            connection.close()
            return True

def run_word2vec_server(trained_model_path):
    # Load Google's pre-trained Word2Vec model.
    model = gensim.models.KeyedVectors.load_word2vec_format(trained_model_path, binary=True)
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ("localhost", 10000)
    print("start up on %s port %s" % server_address)
    sock.bind(server_address)
    sock.listen(5)

    while True:
        print("wait for a connection")
        connection, client_address = sock.accept()
        connection.settimeout(60);
        threading.Thread(target=listen_to_connection, args=(model, connection,client_address)).start()

if __name__ == '__main__':
    run_word2vec_server(trained_model_path="model/GoogleNews-vectors-negative300.bin")
