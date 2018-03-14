import gensim
import socket

def run_word2vec_server(trained_model_path):
    # Load Google's pre-trained Word2Vec model.
    model = gensim.models.KeyedVectors.load_word2vec_format(trained_model_path, binary=True)
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ("localhost", 10001)
    print("start up on %s port %s" % server_address)
    sock.bind(server_address)
    sock.listen(1)

    while True:
        print("wait for a connection")
        connection, client_address = sock.accept()
        sent = False
        try:
            print("connection from ", client_address)
            data = connection.recv(1024)
            words = data.decode('utf-8').strip().split(", ")
            print(words[0], words[1])
            score = round(model.wv.similarity(words[0], words[1]), 4)
            print("score:", score)
            connection.send(str.encode(str(score)))
            sent = True
        except:
            print("no score. sending -1")
            if not sent:
                connection.send(str.encode(str(-1)))
            continue
        finally:
            connection.close()


if __name__ == '__main__':
    run_word2vec_server(trained_model_path="./model/GoogleNews-vectors-negative300.bin")


